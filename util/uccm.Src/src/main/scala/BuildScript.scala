package com.sudachen.uccm

import java.io.File

object BuildConfig extends Enumeration {
  val Debug, Release = Value

  def stringify(c:Value):String = c match {
    case Debug => "debug"
    case Release => "release"
  }

  def fromString(s:String):Option[Value] = s match {
    case "debug" => Some(Debug)
    case "release" => Some(Release)
    case _ => None
  }
}

case class FileCopy(to:String,from:String,replace:List[(String,Map[String,String])=>String]=Nil)
case class BuildScript(boardName:String,
	               mainC:String,
                       ccTool:Compiler.Value,
                       debugger:Option[Debugger.Value],
                       config:BuildConfig.Value,
                       cflags:List[String] = Nil,
                       sources:List[String] = Nil,
                       modules:List[String] = Nil,
                       libraries:List[String] = Nil,
                       generated:List[(String,String)] = Nil,
                       begin:List[String] = Nil,
                       end:List[String] = Nil,
                       ldflags:List[String] = Nil,
                       asflags:List[String] = Nil,
                       softDevice:String = "RAW",
                       jRttViewOpt:List[String] = Nil,
                       debuggerOpt:List[String] = Nil,
                       lets:Map[String,String] = Map(),
                       generatedPart:List[(String,Map[String,String]=>String)] = Nil,
                       copyfile:Map[String,FileCopy] = Map()) {
  def toXML : scala.xml.Node = {
<uccm>
  <board>
    {boardName}
  </board>
  <main>
    {mainC}
  </main>
  <cctool>
    {Compiler.stringify(ccTool)}
  </cctool>
  <debugger>
    {debugger match { case Some(tag) => Debugger.stringify(tag) case None => ""}}
  </debugger>
  <debuggeropt>
    {debuggerOpt map { i =>
    <opt>
      {i}
    </opt>}}
  </debuggeropt>
  <jrttview>
    {jRttViewOpt map { i =>
    <opt>
      {i}
    </opt>}}
  </jrttview>
  <softdevice>
    {softDevice}
  </softdevice>
  <config>
    {BuildConfig.stringify(config)}
  </config>
  <cflags>
    {cflags map { i =>
    <flag>
      {i}
    </flag>} }
  </cflags>
  <ldflags>
    {ldflags map { i =>
    <flag>
      {i}
    </flag>} }
  </ldflags>
  <asflags>
    {asflags map { i =>
    <flag>
      {i}
    </flag>} }
  </asflags>
  <sources>
    {sources map { i =>
    <file>
      {i}
    </file>} }
  </sources>
  <modules>
    {modules map { i =>
    <file>
      {i}
    </file>} }
  </modules>
  <libraries>
    {libraries map { i =>
    <file>
      {i}
    </file>} }
  </libraries>
</uccm>
  }
}

object BuildScript {

  def fromXML(xml: scala.xml.Node) : BuildScript = {
    def bs(c:Char):Boolean = c match { case ' '|'\n'|'\r' => true case _ => false }
    def ns(s:String) = s.dropWhile{bs}.reverse.dropWhile{bs}.reverse
    BuildScript(
      ns((xml\"board").text),
      mainC = ns((xml\"main").text),
      Compiler.fromString(ns((xml\"cctool" ).text)).get,
      Debugger.fromString(ns((xml\"debugger" ).text)),
      BuildConfig.fromString(ns((xml\"config" ).text)).get,
      softDevice = ns((xml\"softdevice" ).text),
      debuggerOpt = (xml\"debuggeropt"\"opt").map{ x => ns(x.text)}.toList,
      jRttViewOpt = (xml\"jrttview"\"opt").map{ x => ns(x.text)}.toList,
      cflags = (xml\"cflags"\"flag").map{ x => ns(x.text)}.toList,
      ldflags = (xml\"ldflags"\"flag").map{ x => ns(x.text)}.toList,
      asflags = (xml\"asflags"\"flag").map{ x => ns(x.text)}.toList,
      sources = (xml\"sources"\"file").map{ x => ns(x.text)}.toList,
      modules = (xml\"modules"\"file").map{ x => ns(x.text)}.toList,
      libraries = (xml\"libraries"\"file").map{ x => ns(x.text)}.toList
    )}

  lazy val uccmDirectory : String  = {
    val rJar = "file:(\\S+.jar)".r
    val rJarOne = "jar:file:(\\S+).jar!.+".r
    val rClass = "file:(\\S+)/".r
    classOf[BuildScript].getProtectionDomain.getCodeSource.getLocation.toURI.toURL.toString match {
      case rJar(path) => new File(path).getParentFile.getAbsolutePath
      case rJarOne(path) => new File(path).getParentFile.getAbsolutePath
      case rClass(path) => new File(path).getAbsoluteFile.getParentFile.getParentFile.getParentFile.getParentFile.getParentFile.getPath
      case p =>
        System.err.println("could not detect uCcm directory ")
        System.exit(1); ""
    }
  }

  lazy val uccmDirectoryFile : File  = new File(uccmDirectory).getAbsoluteFile

  lazy val uccmRepoDirectory: String = {
    sys.env.get("UCCM100REPO") match {
      case Some(path) => path
      case None => sys.env("LOCALAPPDATA") + "\\uCcm100Repo"
    }
  }

  lazy val uccmRepoDirectoryFile: File = new File(uccmRepoDirectory).getAbsoluteFile

  lazy val uccmImportsRepoFile: File = new File("~imports")

  var enableDevOpts: Boolean = true

  var buildDirFile: File = null

}

