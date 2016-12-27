package uccm
import java.io.File
import sys.process._

class UccmPragma
case class UccmHome(tag:String, value:String) extends UccmPragma
case class UccmBoard(tag:String, value:String) extends UccmPragma
case class UccmXcflags(tag:String, value:String) extends UccmPragma
case class UccmCflags( value:String) extends UccmPragma
case class UccmRequire(value:String) extends UccmPragma
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
    val rRequire = "#pragma\\s*uccm\\s*require\\((\\S+)\\)\\s*$".r
    val rFile    = "#pragma\\s*uccm\\s*file\\((\\S+)\\)\\s*\\+?=\\s*(.+)$".r
    val rDefault = "#pragma\\s*uccm\\s*default\\((\\S+)\\)\\s*=\\s*(.+)$".r

    def ps(s: Stream[String]) : Stream[UccmPragma] = s match {
      case xs #:: t => xs match {
        case rXcflags(tag,value) => UccmXcflags(tag,value) #:: ps(t)
        case rBoard(tag,value) => UccmBoard(tag,value) #:: ps(t)
        case rHome(tag,value) => UccmHome(tag,value) #:: ps(t)
        case rCflags(value) => UccmCflags(value) #:: ps(t)
        case rRequire(tag) => UccmRequire(tag) #:: ps(t)
        case rFile(tag,value) => UccmAppend(tag,value) #:: ps(t)
        case rDefault(tag,value) => UccmDefault(tag,value) #:: ps(t)
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
  val Reconfig, Build, Erase, Flash, Connect, Reset = Value
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
                       objDir:String,
                       incDir:String,
                       target:String,
                       generated:List[(String,String)])

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

      opt[Unit]("reconfig").
        action( (_,c) => c.copy(targets = c.targets + Target.Reconfig)).
        text("reconfigure build")

      opt[Unit]("release").
        action( (_,c) => c.copy(buildConfig = BuildConfig.Release)).
        text("build release")

      opt[Unit]("debug").
        action( (_,c) => c.copy(buildConfig = BuildConfig.Debug)).
        text("build debug")

      opt[Unit]("erase").
        action( (_,c) => c.copy(targets = c.targets + Target.Erase)).
        text("erase mCu memory")

      opt[Unit]("flash").
        action( (_,c) => c.copy(targets = c.targets + Target.Flash)).
        text("write firmware into mCu memory")

      opt[Unit]("reset").
        action( (_,c) => c.copy(targets = c.targets + Target.Reset)).
        text("reset mCu")

      opt[Unit]("connect").
        action( (_,c) => c.copy(targets = c.targets + Target.Connect)).
        text("start debugger connected to mCu")

      opt[String]('b',"board").
        action( (x,c) => c.copy(board = Some(x))).
        text("set target board")

      opt[String]('c',"compiler").
        action( (x,c) => c.copy(compiler = Compiler.fromString(x))).
        text("use specified compiler")

      opt[String]('d',"debugger").
        action( (x,c) => c.copy(debugger = Debugger.fromString(x))).
        text("use specified debugger")

      arg[File]("main.c").optional().
        action( (x,c) => c.copy(mainFile = Some(x))).
        text("firmware main.c file")
    }

    cmdlParser.parse(argv, CmdlOptions()) match {
      case Some(cmdlOpts) => act(cmdlOpts)
      case None =>
    }
  }

  def act(cmdlOpts: CmdlOptions) : Unit = {

    val mainFile : File = cmdlOpts.mainFile match {
      case Some(f) => f
      case None => new File("main.c")
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

    if ( defaults.msgs.nonEmpty ) defaults.msgs.foreach { System.err.println }

    val targetBoard: String = cmdlOpts.board.getOrElse(defaults.board.getOrElse("custom"))
    val targetCompiler: Compiler.Value = cmdlOpts.compiler.getOrElse(defaults.compiler)
    val uccmHome = new File("c:/projects/rnd/uccm")

    val boardPragmas : Option[List[UccmPragma]] = preprocessUccmBoardFiles(uccmHome).find {
      x => x.exists {
          case UccmBoard(b,_) if b == targetBoard => true
          case _ => false
      }
    }

    if ( boardPragmas.isEmpty ) {
      System.err.println("unknown board $targetBoard")
      System.exit(1)
    }

    println("uccm is working now for board $targetBoard")
    println("uccm is using $targetCompiler compiler")

    val homes = boardPragmas.get.foldLeft( Map[String,String]() ) {
      (homes,prag) => prag match {
        case UccmHome(tag,path) => homes + (tag -> path)
        case _ => homes
      }
    }

    val xcflags = boardPragmas.get.foldLeft( "-I${uccmHome.getCanonicalPath}" :: Nil ) {
      (xcflags,prag) => prag match {
        case UccmXcflags(x,value) => Compiler.fromString(x) match {
          case Some(c) if c == targetCompiler => value :: xcflags
          case None if x == "*" => value :: xcflags
          case _ => xcflags
        }
        case UccmBoard(x,value) if x == targetBoard => value :: xcflags
        case _ => xcflags
      }
    } map { expandHome(homes,_) } reverse

    val cc = expandHome(homes,Compiler.ccPath(targetCompiler))
    val buildDir = new File(mainFile.getParentFile,s"~Build/$targetBoard")
    val objDir = new File(buildDir,"obj").getCanonicalPath
    val incDir = new File(buildDir,"inc").getCanonicalPath
    val target = new File(buildDir,"firmware").getCanonicalPath
    val tempFile = File.createTempFile("uccm",".gcc.i")
    val exitCode = (cc + " -E " + xcflags.mkString(" ") + " " + mainFile.getCanonicalPath #> tempFile) !

    val buildScript = Pragmas.extractFrom(tempFile).foldLeft( BuildScript(cc,Nil,List(mainFile.getCanonicalPath),objDir,incDir,target,Nil)) {
        (bs,prag) => prag match {
          case UccmXcflags(x,value) => Compiler.fromString(x) match {
            case Some(c) if c == targetCompiler => bs.copy(cflags = value :: bs.cflags)
            case None if x == "*" => bs.copy(cflags = value :: bs.cflags)
            case _ => bs
          }
          case UccmCflags(value) => bs.copy(cflags = value :: bs.cflags)
          case UccmBoard(b,value) if b == targetBoard => bs.copy(cflags = value :: bs.cflags)
          case UccmRequire(value) => bs.copy(sources = value :: bs.sources)
          case UccmAppend(tag,value) => bs.copy(generated = (tag,value) :: bs.generated )
      }
    } match {
      case bs => bs.copy(
        generated = bs.generated.reverse,
        cflags = bs.cflags.map { expandHome(homes,_)} reverse,
        sources = bs.sources.map { expandHome(homes,_)} reverse)
    }

    executeBuildScript(buildScript,cmdlOpts.targets)
  }

  def preprocessUccmBoardFiles(uccmHome:File) : Stream[List[UccmPragma]] = {
    val boardDir = new File(uccmHome,"uccm/board")

    def f(s:Stream[File]) : Stream[List[UccmPragma]] =  s match {
      case xs #:: t => Pragmas.extractFrom(xs).toList #:: f(t)
      case _ => Stream.empty
    }

    if ( !boardDir.exists || !boardDir.isDirectory )
      Stream.empty
    else
      f(boardDir.listFiles.toStream)
  }

  def expandHome(homes:Map[String,String],text:String) : String = ???

  def executeBuildScript(buildScript:BuildScript, targets:Set[Target.Value]): Unit =
  {

  }
}