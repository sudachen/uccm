package uccm
import java.io.File
import java.net.JarURLConnection

import org.apache.commons.io.FileUtils

import sys.process._

class UccmPragma
case class UccmHome(tag:String, value:String) extends UccmPragma
case class UccmBoard(tag:String, value:String) extends UccmPragma
case class UccmXcflags(tag:String, value:String) extends UccmPragma
case class UccmCflags(value:String) extends UccmPragma
case class UccmRequire(tag:String,value:String) extends UccmPragma
case class UccmAppend(tag:String, value:String) extends UccmPragma
case class UccmDefault(tag:String, value:String) extends UccmPragma

object Pragmas {
  def extractFrom(file:File) : Stream[UccmPragma] = {
    val f = io.Source.fromFile(file)

    def js(s: Stream[String]) : Stream[String] = s match  {
      case xs #:: t if xs.endsWith("\\") => (xs.dropRight(1) + js(t).head) #:: js(t).tail
      case xs #:: t => xs #:: js(t)
      case _ => f.close; Stream.empty
    }

    val rXcflags = "#pragma\\s*uccm\\s*xcflags\\((\\S+)\\)\\s*\\+?=\\s*(.+)$".r
    val rBoard   = "#pragma\\s*uccm\\s*board\\((\\S+)\\)\\s*=\\s*(.+)$".r
    val rHome    = "#pragma\\s*uccm\\s*home\\((\\S+)\\)\\s*=\\s*(.+)$".r
    val rCflags  = "#pragma\\s*uccm\\s*cflags\\s*\\+?=\\s*(.+)$".r
    val rRequire = "#pragma\\s*uccm\\s*require\\((\\S+)\\)\\s*=\\s*(.+)$".r
    val rFile    = "#pragma\\s*uccm\\s*file\\((\\S+)\\)\\s*\\+?=\\s*(.+)$".r
    val rDefault = "#pragma\\s*uccm\\s*default\\((\\S+)\\)\\s*=\\s*(.+)$".r

    def ns(s:String) = s.dropRight(s.length-s.lastIndexWhere { _ !=  ' ' }-1)

    def ps(s: Stream[String]) : Stream[UccmPragma] = s match {
      case xs #:: t => xs match {
        case rXcflags(tag,value) => UccmXcflags(tag,ns(value)) #:: ps(t)
        case rBoard(tag,value) => UccmBoard(tag,ns(value)) #:: ps(t)
        case rHome(tag,value) => UccmHome(tag,ns(value)) #:: ps(t)
        case rCflags(value) => UccmCflags(value) #:: ps(t)
        case rRequire(tag,value) => UccmRequire(tag,ns(value)) #:: ps(t)
        case rFile(tag,value) => UccmAppend(tag,ns(value)) #:: ps(t)
        case rDefault(tag,value) => UccmDefault(tag,ns(value)) #:: ps(t)
        case _ => ps(t)
      }
      case _ => Stream.empty
    }

    ps(js(f.getLines().toStream))
  }
}

object BuildConfig extends Enumeration {
  val Debug, Release = Value
}

object Target extends Enumeration {
  val Rebuild, Build, Erase, Flash, Connect, Reset = Value
}

object Compiler extends Enumeration {
  val GCC, ARMCC = Value

  def fromString(name:String): Option[Value] = name match {
    case "armcc" => Some(ARMCC)
    case "gcc" => Some(GCC)
    case _ => None
  }

  def ccPath(kind:Value):String = kind match {
    case ARMCC => "[armcc]\\bin\\armcc.exe"
    case GCC => "[gcc]\\bin\\arm-none-eabi-gcc.exe"
  }
}

object Debugger extends Enumeration {
  val STLINK, JLINK = Value
  def fromString(name:String): Option[Value] = name match {
    case "stlink" => Some(STLINK)
    case "jlink" => Some(JLINK)
    case _ => None
  }
}

case class BuildScript(ccTool:String,
                       cflags:List[String],
                       sources:List[String],
                       modules:List[String],
                       libraries:List[String],
                       generated:List[(String,String)],
                       begin:List[String] = Nil,
                       end:List[String] = Nil ) {
  def toXML : scala.xml.Node = {
    <uccm>
      <cctool>
        {ccTool}
      </cctool>
      <cflags>
        {cflags map { i =>
        <flag>
          {i}
        </flag>} }
      </cflags>
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
      <generated>
        {generated map { i =>
        <append>
          <file>
            {i._1}
          </file>
          <content>
            {scala.xml.PCData(i._2)}
          </content>
        </append>} }
      </generated>
    </uccm>
  }
}

object BuildScript {

  def fromXML(xml: scala.xml.Node) : BuildScript =
    BuildScript(
      (xml \ "cctool" ).text,
      (xml \ "cflags" \ "flag" ) .map { _.text } .toList,
      (xml \ "sources" \ "file" ) .map { _.text } .toList,
      (xml \ "modules" \ "file" ) .map { _.text } .toList,
      (xml \ "libraries" \ "file" ) .map { _.text } .toList,
      (xml \ "generated" \ "append" ) .map { x => ((x\"name").text,(x\"content").text) } .toList
    )
}

case class MainDefaults(board:Option[String],
                        compiler:Compiler.Value,
                        debugger:Debugger.Value,
                        msgs:List[String]) {

  def reverse:MainDefaults = copy(msgs = msgs.reverse)
}

case class CmdlOptions(buildConfig: BuildConfig.Value = BuildConfig.Release,
                       targets: Set[Target.Value] = Set(Target.Build),
                       board: Option[String] = None,
                       compiler: Option[Compiler.Value] = None,
                       debugger: Option[Debugger.Value] = None,
                       mainFile: Option[File] = None)

object Uccm {

  def main(argv: Array[String]): Unit = {
    val cmdlParser = new scopt.OptionParser[CmdlOptions]("uccm") {
      head("uC cortex-m build manager", "1.0")
      help("help").
        text("show this help and exit")

      override def showUsageOnError = true

      opt[Unit]("rebuild").
        action( (_,c) => c.copy(targets = c.targets + Target.Rebuild)).
        text("reconfigure and do clean build")

      opt[Unit]("release").
        action( (_,c) => c.copy(buildConfig = BuildConfig.Release)).
        text("build release")

      opt[Unit]("debug").
        action( (_,c) => c.copy(buildConfig = BuildConfig.Debug)).
        text("build debug")

      opt[Unit]("erase").
        action( (_,c) => c.copy(targets = c.targets + Target.Erase)).
        text("erase uC memory")

      opt[Unit]("flash").
        action( (_,c) => c.copy(targets = c.targets + Target.Flash)).
        text("write firmware into uC memory")

      opt[Unit]("reset").
        action( (_,c) => c.copy(targets = c.targets + Target.Reset)).
        text("reset uC")

      opt[Unit]("connect").
        action( (_,c) => c.copy(targets = c.targets + Target.Connect)).
        text("start debugger connected to uC")

      opt[String]('b',"board").
        action( (x,c) => c.copy(board = Some(x))).
        text("set target board")

      opt[Unit]("gcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.GCC))).
        text("use gcc compiler")

      opt[Unit]("armcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.ARMCC))).
        text("use gcc compiler")

      opt[Unit]("stlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.STLINK))).
        text("use stlink debugger")

      opt[Unit]("jlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.JLINK))).
        text("use jlink debugger")

      arg[File]("main.c").optional().
        action( (x,c) => c.copy(mainFile = Some(x))).
        text("firmware main.c file")
    }

    cmdlParser.parse(argv, CmdlOptions()) match {
      case Some(cmdlOpts) => act(cmdlOpts)
      case None =>
    }
  }

  def getCurrentDirectory : String = new File(".").getCanonicalPath

  def getUccmDirectory : Option[String]  = {
    val rJar = "file:(\\S+.jar)".r
    val rJarOne = "jar:file:(\\S+).jar!.+".r
    val rClass = "file:(\\S+)/".r
    classOf[BuildScript].getProtectionDomain.getCodeSource.getLocation.toURI.toURL.toString match {
      case rJar(path) => Some(new File(path).getParentFile.getAbsolutePath)
      case rJarOne(path) => Some(new File(path).getParentFile.getAbsolutePath)
      case rClass(path) => Some(new File(path).getAbsolutePath)
      case p => { println(p); None }
    }
  }

  def act(cmdlOpts: CmdlOptions) : Unit = {

    println(getUccmDirectory)

    val mainFile : File = cmdlOpts.mainFile match {
      case Some(f) if f.isAbsolute => f
      case Some(f) => f.getAbsoluteFile
      case None => new File("main.c").getAbsoluteFile
    }

    if ( !mainFile.exists ) {
      System.err.println("main file \"" + mainFile.getCanonicalPath + "\" does not exist")
      System.exit(1)
    }

    val defaults = Pragmas.extractFrom(mainFile).foldLeft( MainDefaults(None,Compiler.GCC,Debugger.STLINK,Nil)) {
      ( dflts, prag ) => prag match {
        case UccmDefault(tag,value) => tag match {
          case "board" => dflts.copy(board = Some(value))
          case "compiler" => Compiler.fromString(value) match {
            case Some(x) => dflts.copy(compiler = x)
            case None => dflts.copy(msgs = s"unknown default compiler $value, pragma ignored" :: dflts.msgs)
          }
          case "debugger" => Debugger.fromString(value) match {
            case Some(x) => dflts.copy(debugger = x)
            case None => dflts.copy(msgs = s"unknown default debugger $value, pragma ignored" :: dflts.msgs)
          }
          case _ => dflts.copy(msgs = s"unknown default $tag, pragma ignored" :: dflts.msgs)
        }
    }} .reverse

    defaults.msgs.foreach { System.err.println }

    val targetBoard: String = cmdlOpts.board.getOrElse(defaults.board.getOrElse("custom"))
    val targetCompiler: Compiler.Value = cmdlOpts.compiler.getOrElse(defaults.compiler)
    val uccmHome = new File("c:/projects/rnd/uccm")

    val boardPragmas : List[UccmPragma] = preprocessUccmBoardFiles(targetBoard,uccmHome)

    if ( boardPragmas.isEmpty ) {
      System.err.println(s"unknown board $targetBoard")
      System.exit(1)
    }

    println(s"uccm is working now for board $targetBoard")
    println(s"uccm is using $targetCompiler compiler")

    val buildDir = new File(mainFile.getParentFile,
      if ( cmdlOpts.buildConfig == BuildConfig.Release ) s"~Release/$targetBoard"
      else s"~Debug/$targetBoard"
    )

    if ( !buildDir.exists ) buildDir.mkdirs()

    val objDir = new File(buildDir,"obj")
    val incDir = new File(buildDir,"inc")

    def expandEnv(s:String):String = "(\\%(\\w+)\\%)".r findFirstMatchIn s match {
      case Some(m) => expandEnv(s.replace(m.group(1),sys.env.getOrElse(m.group(2),m.group(2))))
      case None => s
    }

    val homes = boardPragmas.foldLeft( Map[String,String]() ) {
      (homes,prag) => prag match {
        case UccmHome(tag,path) => homes + (tag -> expandEnv(path))
        case _ => homes
      }
    } + ("inc" -> incDir.getCanonicalPath) + ("src" -> mainFile.getParentFile.getCanonicalPath)

    homes.foreach { (p) => println(s"${p._1} => ${p._2}")}

    def expandHome(s:String):String = "(\\[(\\w+)\\])".r findFirstMatchIn s match {
      case Some(m) => expandHome(s.replace(m.group(1),homes.getOrElse(m.group(2),m.group(2))))
      case None => s
    }

    val xcflags = boardPragmas.foldLeft( s"-I${uccmHome.getCanonicalPath}" :: Nil ) {
      (xcflags,prag) => prag match {
        case UccmXcflags(x,value) => Compiler.fromString(x) match {
          case Some(`targetCompiler`) => value :: xcflags
          case None if x == "*" => value :: xcflags
          case _ => xcflags
        }
        case UccmBoard(`targetBoard`,value) => value :: xcflags
        case UccmBoard("*",value) => value :: xcflags
        case _ => xcflags
      }
    } .map{expandHome} .reverse

    val cc = expandHome(Compiler.ccPath(targetCompiler))

    if ( cmdlOpts.targets.contains(Target.Rebuild) && buildDir.exists ) {
      FileUtils.deleteDirectory(objDir)
      FileUtils.deleteDirectory(incDir)
    }

    if ( !objDir.exists ) objDir.mkdirs()
    if ( !incDir.exists ) incDir.mkdirs()

    val buildScriptFile = new File(buildDir,"script.xml")

    def extractFromPreprocessor = {
      val buffer = new StringBuffer()
      val tempFile = File.createTempFile("uccm",".gcc.i")
      val mainFilePath = mainFile.getCanonicalPath
      val gccPreprocCmdline = cc + " -E " + xcflags.map{expandHome}.mkString(" ") + " " + mainFilePath
      println(gccPreprocCmdline)

      val errCode = (gccPreprocCmdline #> tempFile) ! ProcessLogger{buffer append _}
      if ( errCode != 0 ) {
        System.err.println(buffer)
        System.err.println("failed to preprocess main C-file")
        System.exit(1)
      }

      Pragmas.extractFrom(tempFile).foldLeft( BuildScript(cc,Nil,List(mainFilePath),Nil,Nil,Nil)) {
        (bs,prag) => prag match {
          case UccmXcflags(x,value) => Compiler.fromString(x) match {
            case Some(`targetCompiler`) => bs.copy(cflags = value :: bs.cflags)
            case None if x == "*" => bs.copy(cflags = value :: bs.cflags)
            case _ => bs
          }
          case UccmCflags(value) => bs.copy(cflags = value :: bs.cflags)
          case UccmBoard(`targetBoard`,value) => bs.copy(cflags = value :: bs.cflags)
          case UccmRequire("module",value) => bs.copy(modules = value :: bs.modules)
          case UccmRequire("source",value) => bs.copy(sources = value :: bs.sources)
          case UccmRequire("lib",value) => bs.copy(libraries = value :: bs.libraries)
          case UccmRequire("begin",value) => bs.copy(begin = value :: bs.begin)
          case UccmRequire("end",value) => bs.copy(end = value :: bs.end)
          case UccmAppend(tag,value) => bs.copy(generated = (tag,value) :: bs.generated )
          case _ => bs
        }
      } match {
        case bs => bs.copy(
          generated = bs.generated.reverse,
          cflags = bs.cflags.map{expandHome}.reverse,
          sources = bs.begin.map{expandHome}.reverse ++
                    bs.sources.map{expandHome}.reverse ++
                    bs.end.map{expandHome},
          modules = bs.modules.map{expandHome}.reverse,
          libraries = bs.libraries.map{expandHome}.reverse
        )
      }
    }

    val buildScript =
      if ( !buildScriptFile.exists || cmdlOpts.targets.contains(Target.Rebuild) )
        extractFromPreprocessor
      else
        BuildScript.fromXML(scala.xml.XML.loadFile(buildScriptFile))

    if ( !buildScriptFile.exists || cmdlOpts.targets.contains(Target.Rebuild) )
      scala.xml.XML.save(buildScriptFile.getCanonicalPath,buildScript.toXML)

    executeBuildScript(buildScript,cmdlOpts.targets)
  }

  def preprocessUccmBoardFiles(targetBoard:String,uccmHome:File) : List[UccmPragma] = {
    val boardDir = new File(uccmHome,"uccm/board")

    def f(s:Stream[File]) : Stream[List[UccmPragma]] =  s match {
      case xs #:: t => Pragmas.extractFrom(xs).toList #:: f(t)
      case _ => Stream.empty
    }

    val boardPragmas: Option[List[UccmPragma]] = {
      if ( !boardDir.exists || !boardDir.isDirectory )
        Stream.empty
      else
        f(boardDir.listFiles.toStream)
    } find {
        x => x.exists {
          case UccmBoard(`targetBoard`,_) => true
          case _ => false
        }
    }

    if ( boardPragmas.nonEmpty )
      Pragmas.extractFrom(new File(uccmHome,"uccm/uccm.h")).toList ++ boardPragmas.get
    else
      Nil
  }

  def executeBuildScript(buildScript:BuildScript, targets:Set[Target.Value]): Unit =
  {

  }
}