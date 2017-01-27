package com.sudachen.uccm

import java.io.{File, FileOutputStream, FileWriter, PrintWriter}
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success, Try}
import sys.process._

object PakType extends Enumeration {
  val Setup, Archive, QtArchive = Value

  def fromString(s:String):Value = s.toLowerCase match {
    case "setup" => Setup
    case "archive" => Archive
    case "qtarchive" => QtArchive
  }

  def stringify(kind:Value):String = kind match {
    case Setup => "setup"
    case Archive => "archive"
    case QtArchive => "qtarchive"
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
{
  def toXML : xml.Node = {
<component>
  <name>{name}</name>
  <root>{root.getOrElse("")}</root>
  <description>{description}</description>
  <type>{PakType.stringify(pakType)}</type>
  <rev>{rev}</rev>
  <args>{args}</args>
  <file>{if (file.nonEmpty) scala.xml.PCData(file) else ""}</file>
  <current>{if (isCurrentRev) "yes" else ""}</current>
  {download.map { url:String =>
  <download>{url}</download>
  }}
</component>
  }
}

class Components(val repoDir:File, val catalog: Map[String, List[ComponentInfo]]) {

  def mkIsGood(cinfo: ComponentInfo):File = new File(repoDir,s".${cinfo.name}-${cinfo.rev}.xml")
  def mkPakDir(cinfo: ComponentInfo):File = new File(repoDir,s"${cinfo.name}-${cinfo.rev}")

  lazy val repoDownloadDir:File = new File(repoDir,".downloads")

  def setup(file:File, cinfo:ComponentInfo) : Try[Unit] = Try {
    BuildConsole.info(s"installing ${cinfo.name}")

    val pat = "%UCCM100REPO%"

    def expand(s:String) : Stream[String] = {
      val t = BuildScript.uccmRepoDirectory.map{case '\\' => '/' case x => x}
      s.indexOf(pat) match {
        case n if n >= 0 =>
          (s.substring(0,n) + t) #:: expand(s.substring(n+pat.length))
        case _ => s #:: Stream.empty
      }
    }

    val f = File.createTempFile("uccm-",".txt")

    val args = if ( cinfo.args.contains("&file&")) {
      f.deleteOnExit()
      val fw = new FileWriter(f)
      expand(cinfo.file).foreach { x => fw.write(x) }
      fw.close()
      cinfo.args.replace("&file&",f.getAbsolutePath)
    } else cinfo.args

    val cmdl = "\""+file.getAbsolutePath+"\" " + args
    BuildConsole.verbose(cmdl)
    if ( 0 != cmdl.! )
      throw new RuntimeException("failed to install")
  }

  def unpack(file:File, cinfo:ComponentInfo) : Try[Unit] = Try {
    val fIsGood = mkIsGood(cinfo)
    if (fIsGood.exists) fIsGood.delete()
    val dir = mkPakDir(cinfo)
    if (dir.exists) FileUtils.deleteDirectory(dir)

    val z7a = BuildScript.uccmDirectory + "\\util\\7za.exe"
    val cmdl = List(z7a,"x","-y","-O\""+dir.getAbsolutePath+"\"","\""+file.getAbsolutePath+"\"").mkString(" ")
    val pl = ProcessLogger(s=>Unit,s=>Unit)
    BuildConsole.info(s"unpaking ${cinfo.name}")
    BuildConsole.verbose(cmdl)
    if ( 0 != (cmdl!pl) )
      throw new RuntimeException("7-zip was failed")
    val fIsGoodTmp = new File(fIsGood.getPath+".tmp")
    xml.XML.save(fIsGoodTmp.getPath,cinfo.toXML)
    fIsGoodTmp.renameTo(fIsGood)
  }

  def qtUnpack(file:File, cinfo:ComponentInfo) : Try[Unit] = Try {
    val fIsGood = mkIsGood(cinfo)
    if (fIsGood.exists) fIsGood.delete()
    val dir = mkPakDir(cinfo)
    if (dir.exists) FileUtils.deleteDirectory(dir)
    val tmpDir = new File(dir,".tmp")
    tmpDir.mkdir()

    val pl = ProcessLogger(s=>Unit,s=>println(s))
    val z7a = BuildScript.uccmDirectory + "\\util\\7za.exe"
    val cmdl1 = List(z7a,"x","-y","-t#","-O\""+tmpDir.getAbsolutePath+"\"","\""+file.getAbsolutePath+"\"").mkString(" ")
    BuildConsole.verbose(cmdl1)
    if ( 0 != (cmdl1!pl) )
      throw new RuntimeException("7-zip was failed on stage 1")
    tmpDir.listFiles{_.getName.endsWith(".7z")}.foreach{ f =>
      val cmdl3 = List(z7a,"x","-y","-O\""+dir.getAbsolutePath+"\"","\""+f.getAbsolutePath+"\"").mkString(" ")
      BuildConsole.verbose(cmdl3)
      if ( 0 != (cmdl3!pl) )
        throw new RuntimeException("7-zip was failed on stage 3")
    }

    FileUtils.deleteDirectory(tmpDir)

    val fIsGoodTmp = new File(fIsGood.getPath+".tmp")
    xml.XML.save(fIsGoodTmp.getPath,cinfo.toXML)
    fIsGoodTmp.renameTo(fIsGood)
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
          lst.collectFirst { case x if x.rev == r => x }
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
          cinfo.download.map { url =>
            Try(Util.download(url,repoDownloadDir)) match {
              case Failure(e) =>
                BuildConsole.stackTrace(e.getStackTrace)
                BuildConsole.error(e.getMessage)
                None
              case Success(f) => Some(f)
            }
          }.collectFirst {
            case Some(file) =>
              val ok = (cinfo.pakType match {
                case PakType.Archive => unpack(file,cinfo)
                case PakType.Setup => setup(file,cinfo)
                case PakType.QtArchive => qtUnpack(file,cinfo)
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
        if (!fIsGood.exists || !fPakDir.exists){
          None
        }else if (pak.root.isEmpty) {
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

  lazy val dfltXmlFile = new File(BuildScript.uccmDirectoryFile,"components.xml")
  lazy val dflt: Components = loadFrom(BuildScript.uccmRepoDirectoryFile,dfltXmlFile) match {
    case Failure(e) => BuildConsole.panic(e.getMessage); null
    case Success(c) => c
  }

}
