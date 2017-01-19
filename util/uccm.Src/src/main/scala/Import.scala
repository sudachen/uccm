
package com.sudachen.uccm
import scala.util.{Try,Success,Failure}
import java.io.File

object Import {

  case class State(imports: List[Import] = Nil, mark:Set[String] = Set()) {}

  def fromXML(n:xml.Node):Import = {
    Import(
      name = Util.ns((n\"name").text),
      ghUser = Util.ns((n\"ghuser").text),
      branch = Util.ns((n\"branch").text)
    )
  }

  def cacheToXML(imports:List[Import]): xml.Node = {
<imports>
  {
    imports.map{
    _.toXML
    }
  }
</imports>
  }

  def cacheFromXML(n:xml.Node): List[Import] = {
    (n\"import").map{fromXML}.toList
  }

  lazy val cacheFile: File = {
    if (!BuildScript.uccmImportsRepoFile.exists) BuildScript.uccmImportsRepoFile.mkdir()
    new File(BuildScript.uccmImportsRepoFile,"cache.xml")
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
    if (!BuildScript.uccmImportsRepoFile.exists) BuildScript.uccmImportsRepoFile.mkdir()
    val ndr = new File(BuildScript.uccmImportsRepoFile,".download")
    if (!ndr.exists) ndr.mkdir()
    ndr
  }

  def acquireImport(imp: Import): Unit = {
    val url = s"https://github.com/${imp.ghUser}/${imp.name}/archive/${imp.branch}.zip"
    val f = Util.download(url,downloadDirFile,Some(s"${imp.ghUser}-${imp.name}-${imp.branch}.zip"))
    Util.unpackZip(f,new File(BuildScript.uccmImportsRepoFile,imp.ghUser))
  }

  def importAll(f: File, importState: State = State()): State = {
    Pragma.extractFrom(f).foldLeft(importState) {
      (is, x) => x match {
        case Pragma.Import(ghUser,name) =>
          val m = name+"@"+ghUser
          if ( is.mark.contains(m) )
            is
          else {
            val cache = loadCache()
            cache.find { i => i.ghUser == ghUser && i.name == name } match {
              case Some(imp) =>
                importAll(imp.importFile, is.copy(imports = imp :: is.imports, mark = is.mark + m))
              case None =>
                val imp = Import(name, ghUser, "master")
                val isc = is.copy(imports = imp :: is.imports, mark = is.mark + m)
                if (imp.localDirFile.isEmpty) {
                  acquireImport(imp)
                  saveCache(imp :: cache)
                }
                importAll(imp.importFile, isc)
            }
          }
        case _ => is
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

  lazy val localDirFile: Option[File] = sys.env.get("UCCM_DEV_"+ghUser.toUpperCase) match {
    case Some(dirName)
      if BuildScript.enableDevOpts && new File(dirName,name).isDirectory =>
        Some(new File(dirName,name))
    case _ => None
  }

  lazy val dirFile: File = localDirFile match {
    case Some(f) => f
    case None => new File(BuildScript.uccmImportsRepoFile,s"$ghUser/$name-$branch")
  }

  lazy val importFile: File = new File(dirFile,"import.h")
}
