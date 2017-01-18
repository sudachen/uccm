
package com.sudachen.uccm
import scala.util.{Try,Success,Failure}
import java.io.File

case class ImportState(cache: List[Import], imports: List[Import] = Nil, mark:Set[String] = Set()) {}

object Import {

  def fromXML(n:xml.Node):Import = {
    Import(
      name = Util.ns((n\"name").text),
      ghUser = Util.ns((n\"ghuser").text),
      branch = Util.ns((n\"branch").text)
    )
  }

  def cacheToXML(imports:List[Import]): xml.Node = {
    <imports>
      {imports.map{_.toXML}}
    </imports>
  }

  def cacheFromXML(n:xml.Node): List[Import] = {
    (n\"import").map{fromXML}.toList
  }

  lazy val importsDirFile = new File("~imports")
  lazy val cacheFile: File = {
    if (!importsDirFile.exists) importsDirFile.mkdir()
    new File(importsDirFile,"cache.xml")
  }

  def loadCache() : List[Import] = {
    if ( cacheFile.exists )
      cacheFromXML(xml.XML.loadFile(cacheFile))
    else
      Nil
  }

  def saveCache(cache: List[Import]) : Unit = {
    if ( cacheFile.exists ) cacheFile.delete()
    xml.XML.save(cacheFile.getPath,cacheToXML(cache))
  }

  lazy val downloadDirFile: File = {
    if (!importsDirFile.exists) importsDirFile.mkdir()
    val ndr = new File(importsDirFile,".download")
    if (!ndr.exists) ndr.mkdir()
    ndr
  }

  def acquireImport(imp: Import): Unit = {
    val url = s"https://github.com/${imp.ghUser}/${imp.name}/archive/${imp.branch}.zip"
    val f = Util.download(url,downloadDirFile)
    val dirFile = new File(s"~imports/${imp.ghUser}")
    Util.unpackZip(f,dirFile)
  }

  def importAll(f: File, importState: ImportState): ImportState = {
    Pragmas.extractFrom(f).foldLeft(importState) {
      (is, x) => x match {
        case UccmImport(ghUser,name) =>
          val m = name+"@"+ghUser
          if ( is.mark.contains(m) )
            is
          else
            is.cache.find { i => i.ghUser == ghUser && i.name == name } match {
              case Some(imp) =>
                importAll(imp.importFile, is.copy( imports = imp :: is.imports, mark = is.mark + m))
              case None =>
                val imp = Import(name,ghUser,"master")
                val isc = is.copy( imp :: is.cache, imports = imp :: is.imports, mark = is.mark + m)
                acquireImport(imp)
                saveCache(isc.cache)
                importAll(imp.importFile, isc)
            }
      }
    }
  }
}

case class Import(name:String,ghUser:String,branch:String) {
  def toXML: xml.Node = {
    <import>
      <name>{name}</name>
      <ghuser>{ghUser}</ghuser>
      <branch>{branch}</branch>
    </import>
  }
  def importFile: File = new File(s"~imports/$ghUser/$name-$branch/import.h")
  def dirFile: File = new File(s"~imports/$ghUser/$name-$branch")
}
