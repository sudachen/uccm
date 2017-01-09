package com.sudachen.uccm.components

import java.io.{File, FileOutputStream}
import java.net.URL
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

case class ComponentInfo(name:String,home:String,download:List[String],root:Option[String],description:String,pakType:PakType.Value)

class Components(val catalog: Map[String,ComponentInfo]) {

  def mkIsGood(name:String):File = new File(uccmRepo,".pk-$name-is-good.txt")
  def mkPakDir(name:String):File = new File(uccmRepo,"pk-$name")

  lazy val uccmRepo:String = ""
  lazy val uccmRepoFile:File = new File(uccmRepo)

  def download(url:String) : Try[File] = {
    val pakFile = new File("")
    Try(new FileOutputStream(pakFile)) match {
      case Failure(e) => Failure[File](e)
      case Success(os) =>
        val r = Try(new URL(url).openConnection.getInputStream) match {
          case Failure(e) => Failure[File](e)
          case Success(is) =>
            val bf = Array.ofDim[Byte](1024*64)
            def b():Stream[Int] = {
              is.read(bf, 0, bf.length) match {
                case -1 => Stream.empty
                case i => i #:: b()
              }
            }
            val rr = Try(b().foreach{x => os.write(bf,0,x)}) match {
              case Failure(e) => Failure[File](e)
              case _ => Success(pakFile)
            }
            is.close()
            rr
        }
        os.close()
        r
    }
  }

  def setup(file:File) : Try[Unit] = Try {
  }

  def unpack(file:File, dir:File) : Try[Unit] = Try {
    val zf = new ZipFile(file)
    zf.entries.asScala.foreach { r =>
      val f = new File(dir,r.getName)
      if ( r.isDirectory )
        f.mkdirs()
      else {
        val is = zf.getInputStream(r)
        val os = new FileOutputStream(f)
        try {
          val bf = Array.ofDim[Byte](64*1024)
          def read:Stream[Int] = is.read(bf) match { case -1 => Stream.empty case n => n #:: read }
          read.foreach { n => os.write(bf,0,n) }
        } finally {
          os.close()
          is.close()
        }
      }
    }
  }

  def acquireComponent(name:String) : Boolean = {
    if ( !catalog.contains(name) )
      false
    else getComponentHome(name) match {
      case Some(_) => true
      case None =>
        catalog(name).download.map{download}.collectFirst{
          case Success(tmpFile) =>
            val ok = (catalog(name).pakType match {
              case PakType.Archive => unpack (tmpFile, mkPakDir(name))
              case PakType.Setup => setup(tmpFile)
            }) match {
              case Failure(e) =>
                System.err.println(e.getMessage)
                false
              case _ => true
            }
            tmpFile.delete()
            ok
        }.getOrElse(false)
    }
  }

  def getComponentHome(name:String) : Option[String] = {
    if (!catalog.contains(name) || catalog(name).pakType == PakType.Setup )
      None
    else {
      val pak = catalog(name)
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
        val fIsGood = mkIsGood(name)
        val fPakDir = mkPakDir(name)
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
  def loadFrom(file:File): Try[Components] = Try {
    def bs(c:Char):Boolean = c match { case ' '|'\n'|'\r' => true case _ => false }
    def ns(s:String) = s.dropWhile{bs}.reverse.dropWhile{bs}.reverse
    val catalog = xml.XML.loadFile(file)
    val map = (catalog\"component").foldLeft(Map[String,ComponentInfo]()){
      (map,x) => {
        val name = (x\"name").text
        map + ( name -> ComponentInfo(
          name,
          home = ns((x\"althome").text),
          download = (x\"download").map{x => ns(x.text)}.toList,
          root = ns((x\"root").text) match { case "" => None case s:String => Some(s) },
          description = ns((x\"description").text),
          pakType = PakType.fromString(ns((x\"type").text))
        ))
      }
    }
    new Components(map)
  }
}