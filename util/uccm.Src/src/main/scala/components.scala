package com.sudachen.uccm.components

import java.io.{File, FileOutputStream, FileWriter}
import java.net.{HttpURLConnection, URL}
import java.util.zip.ZipFile

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object PakType extends Enumeration {
  val Setup, Archive = Value

  def fromString(s:String):Value = s.toLowerCase match {
    case "setup" => Setup
    case "archive" => Archive
  }
}

case class ComponentInfo( name:String,home:String,
                          download:List[String],
                          root:Option[String],
                          description:String,
                          pakType:PakType.Value,
                          args:String,
                          rev:String,
                          file:String,
                          isCurrentRev:Boolean)

class Components(val repoDir:File, val catalog: Map[String, List[ComponentInfo]]) {

  def mkIsGood(cinfo: ComponentInfo):File = new File(repoDir,s".${cinfo.name}-${cinfo.rev}.xml")
  def mkPakDir(cinfo: ComponentInfo):File = new File(repoDir,s"${cinfo.name}-${cinfo.rev}")

  lazy val repoDownloadDir:File = new File(repoDir,".downloads")

  def download(url:String) : Try[File] = Try {
    val dst = new File(repoDownloadDir,new File(url).getName)
    val dstSha1 = new File(dst.getPath+".sha1")
    val dstPart = new File(dst.getPath+".part")
    if ( !dst.exists || !dstSha1.exists ) {
      if ( dstPart.exists ) dstPart.delete()
      System.out.print(s"connecting => $url")
      System.out.flush()
      val con = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
      try {
        val is = con.getInputStream
        val length = con.getContentLength
        val bf = Array.ofDim[Byte](1024 * 1024)

        def read: Stream[Int] = is.read(bf, 0, bf.length) match {
          case -1 => Stream.empty
          case i => i #:: read
        }

        val sha1 = java.security.MessageDigest.getInstance("SHA-1")
        val os = new FileOutputStream(dstPart)
        try {
          System.out.print("\ndownloading ...")
          System.out.flush()
          val size = read.foldLeft(0) {
            (q, x) => {
              os.write(bf, 0, x)
              if (length > 0) {
                val step = Math.max(length / 50, 1)
                val dif = (q + x) / step - q / step
                if (dif > 0) {
                  System.out.print("#" * dif)
                  System.out.flush()
                }
              }
              sha1.update(bf, 0, x)
              q + x
            }
          }
          System.out.print(s" success! $size")
        } finally {
          System.out.println()
          os.close()
        }

        dstPart.renameTo(dst)

        val sos = new FileWriter(dstSha1)
        try {
          sos.write(sha1.digest().map {
            "%02x".format(_)
          }.mkString + "\n")
        } finally {
          sos.close()
        }

      } finally {
        con.disconnect()
      }
    }
    dst
  }

  def setup(file:File, cinfo:ComponentInfo) : Try[Unit] = Try {
  }

  def unpack(file:File, cinfo:ComponentInfo) : Try[Unit] = Try {
    val dir = mkPakDir(cinfo)
    val zf = new ZipFile(file)
    try {
      zf.entries.asScala.foreach { r =>
        val f = new File(dir, r.getName)
        if (r.isDirectory)
          f.mkdirs()
        else {
          val is = zf.getInputStream(r)
          val os = new FileOutputStream(f)
          try {
            val bf = Array.ofDim[Byte](64 * 1024)
            def read: Stream[Int] = is.read(bf) match {
              case -1 => Stream.empty
              case n => n #:: read
            }
            read.foreach { n => os.write(bf, 0, n) }
          } finally {
            os.close()
            is.close()
          }
        }
      }
    } finally {
      zf.close()
    }
  }

  def queryRevision(name:String,rev:Option[String]) : Option[ComponentInfo] = {
    if ( !catalog.contains(name) )
        None
    else {
      val lst = catalog(name)
      rev match {
        case None =>
          if ( lst.length == 1 ) Some(lst.head)
          else lst.collectFirst { case x if x.isCurrentRev => x }
        case Some(r) =>
          lst.collectFirst { case x if x.rev.equals(r) => x }
      }
    }
  }

  def acquireComponent(name:String,rev:Option[String]=None) : Boolean = {
    if ( !catalog.contains(name) )
      false
    else getComponentHome(name,rev) match {
      case Some(_) => true
      case None => queryRevision(name,rev) match {
        case None => false
        case Some(cinfo) =>
          cinfo.download.map {
            download
          }.collectFirst {
            case Success(file) =>
              val ok = (cinfo.pakType match {
                case PakType.Archive => unpack(file,cinfo)
                case PakType.Setup => setup(file,cinfo)
              }) match {
                case Failure(e) =>
                  System.err.println(e.getMessage)
                  false
                case _ => true
              }
              ok
          }.getOrElse(false)
      }
    }
  }

  def getComponentHome(name:String,rev:Option[String]=None) : Option[String] = {
    val cinfo = queryRevision(name,rev)
    if ( cinfo.isEmpty || cinfo.get.pakType == PakType.Setup )
      None
    else {
      val pak = cinfo.get
      val envHome = "(\\%([\\|\\w]+)\\%)".r findFirstMatchIn pak.home match {
        case Some(m) =>
          m.group(2).split('|').toList.dropWhile { x => !sys.env.contains(x) } match {
            case e :: _ => Some(pak.home.replace(m.group(1), sys.env(e)))
            case Nil => None
          }
        case None => None
      }
      if (envHome.isDefined && new File(envHome.get).exists)
        envHome
      else {
        val fIsGood = mkIsGood(pak)
        val fPakDir = mkPakDir(pak)
        if (!fIsGood.exists || !fPakDir.exists)
          None
        else if (pak.root.isEmpty) {
          Some(fPakDir.getAbsolutePath)
        } else {
          val fRootDir = new File(fPakDir, pak.root.get)
          if (fRootDir.exists)
            Some(fRootDir.getAbsolutePath)
          else
            None
        }
      }
    }
  }
}

object Components {
  def loadFrom(repoDir:File,file:File): Try[Components] = Try {
    def bs(c:Char):Boolean = c match { case ' '|'\n'|'\r' => true case _ => false }
    def ns(s:String) = s.dropWhile{bs}.reverse.dropWhile{bs}.reverse
    val catalog = xml.XML.loadFile(file)
    val map = (catalog\"component").foldLeft(Map[String,List[ComponentInfo]]()){
      (map,x) => {
        val name = (x\"name").text
        map + ( name -> (ComponentInfo(
          name,
          home = ns((x\"althome").text),
          download = (x\"download").map{x => ns(x.text)}.toList,
          root = ns((x\"root").text) match { case "" => None case s:String => Some(s) },
          description = ns((x\"description").text),
          pakType = PakType.fromString(ns((x\"type").text)),
          args = ns((x\"args").text),
          file = (x\"file").text,
          rev = ns((x\"rev").text) match { case "" => "XXX" case r => r },
          isCurrentRev = ns((x\"current").text).nonEmpty
        ) :: map.getOrElse(name,Nil)) )
      }
    }
    new Components(repoDir,map)
  }
}