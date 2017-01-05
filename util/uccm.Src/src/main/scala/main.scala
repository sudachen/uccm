package com.sudachen.uccm
import java.io.{File, FileWriter}
import org.apache.commons.io.FileUtils
import scala.util.{Try,Success,Failure}
import sys.process._
import compiler.Compiler
import debugger.Debugger
import buildscript.BuildScript
import buildscript.BuildConfig
import qteproj.QtProj
import pragmas._

object Target extends Enumeration {
  val Clean, Rebuild, Build, Erase, Upload, Connect, Reset, Qte = Value
}

case class Component ( home: Option[String] = None,
                       info: Option[String] = None,
                       download:Option[String] = None )

case class MainDefaults(board:Option[String],
                        compiler:Compiler.Value,
                        debugger:Debugger.Value,
                        msgs:List[String]) {

  def reverseMsgs:MainDefaults = copy(msgs = msgs.reverse)
}

case class CmdlOptions(buildConfig: BuildConfig.Value = BuildConfig.Release,
                       targets:  Set[Target.Value] = Set(Target.Build),
                       board:    Option[String] = None,
                       compiler: Option[Compiler.Value] = None,
                       debugger: Option[Debugger.Value] = None,
                       mainFile: Option[File] = None,
                       verbose:  Boolean = false,
                       cflags:  List[String] = Nil
                      )

object Prog {

  def main(argv: Array[String]): Unit = {
    val cmdlParser = new scopt.OptionParser[CmdlOptions]("uccm") {
      head("Alexey Sudachen' uC cortex-m build manager, goo.gl/a9irI7")
      help("help").
        text("show this help and exit")

      override def showUsageOnError = true

      opt[Unit]('v',"verbose").
        action( (_,c) => c.copy(verbose = true)).
        text("verbose output")

      opt[Unit]("build").
        action( (_,c) => c.copy(targets = c.targets + Target.Build)).
        text("build project (by default)")

      opt[Unit]("no-build").
        action( (_,c) => c.copy(targets = c.targets - Target.Build)).
        text("skip project build action")

      opt[Unit]("clean").
        action( (_,c) => c.copy(targets = c.targets + Target.Clean - Target.Build)).
        text("remove intermediate files and target firmware")

      opt[Unit]("erase").
        action( (_,c) => c.copy(targets = c.targets + Target.Erase)).
        text("erase uC memory")

      opt[Unit]("upload").
        action( (_,c) => c.copy(targets = c.targets + Target.Upload)).
        text("reprogram uC flash memory")

      opt[Unit]("reset").
        action( (_,c) => c.copy(targets = c.targets + Target.Reset)).
        text("reset uC")

      opt[Unit]("connect").
        action( (_,c) => c.copy(targets = c.targets + Target.Connect)).
        text("try connect to uC")

      opt[String]('b',"board").
        action( (x,c) => c.copy(board = Some(x))).
        text("build for board")

      opt[Unit]("qte").
        action( (_,c) => c.copy(targets = c.targets + Target.Qte - Target.Build)).
        text("generate QTcreator generic project")

      opt[Unit]("rebuild").
        action( (_,c) => c.copy(targets = c.targets + Target.Rebuild)).
        text("reconfigure and do clean build")

      opt[Unit]("release").
        action( (_,c) => c.copy(buildConfig = BuildConfig.Release)).
        text("[on rebuild] configure for release build (by default)")

      opt[Unit]("debug").
        action( (_,c) => c.copy(buildConfig = BuildConfig.Debug)).
        text("[on rebuild] configure for debug build")

      opt[String]('D',"define").
        action( (x,c) => c.copy(cflags = ("-D"+x)::c.cflags)).
        text("[on rebuild] add macro definition to compiler cflags")

      opt[Unit]("gcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.GCC))).
        text("[on rebuild] use ARM-NONE-EABI GNU C compiler")

      opt[Unit]("armcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.ARMCC))).
        text("[on rebuild] use KeilV5 armcc compiler")

      opt[Unit]("armcc-microlib").
        action( (_,c) => c.copy(compiler = Some(Compiler.ARMCC),cflags = "-DUSE_MICROLIB"::c.cflags)).
        text("[on rebuild] use KeilV5 armcc compiler with C-microlib")

      opt[Unit]("stlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.STLINK))).
        text("[on rebuild] use STM ST-Link debugger/programmer")

      opt[Unit]("jlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.JLINK))).
        text("[on rebuild] use SEGGER J-Link debugger/programmer")

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
      case p => println(p); None
    }
  }

  def panic(text:String) : Unit = {
    System.err.println(text)
    System.exit(1)
  }

  def quote(s:String):String = '"' + s + '"'

  def act(cmdlOpts: CmdlOptions) : Unit = {

    val mainFile : File = cmdlOpts.mainFile match {
      case Some(f) => new File(f.getName)
      case None => new File("./main.c")
    }

    val projectDir = mainFile.getParentFile
    def verbose(s:String) = if (cmdlOpts.verbose) println(s)

    if ( !mainFile.exists )
      panic("main file \"" + mainFile.getCanonicalPath + "\" does not exist")

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
        case _ => dflts
    }} .reverseMsgs

    defaults.msgs.foreach { System.err.println }

    val targetBoard: String = cmdlOpts.board.getOrElse(defaults.board.getOrElse("custom"))
    val targetCompiler: Compiler.Value = cmdlOpts.compiler.getOrElse(defaults.compiler)
    val targetDebugger: Debugger.Value = cmdlOpts.debugger.getOrElse(defaults.debugger)
    val uccmHome = new File("c:/projects/rnd/uccm")

    val boardPragmas : List[UccmPragma] = preprocessUccmBoardFiles(targetBoard,uccmHome)

    if ( boardPragmas.isEmpty )
      panic(s"unknown board $targetBoard")

    println(s"uccm is working now for board $targetBoard")

    val buildDir = new File(".",
      //if ( cmdlOpts.buildConfig == BuildConfig.Release ) s"~Release/$targetBoard"
      //else s"~Debug/$targetBoard"
      s"~$targetBoard-${mainFile.getName.take(mainFile.getName.lastIndexOf("."))}"
    )

    if ( !buildDir.exists ) buildDir.mkdirs()
    val objDir = new File(buildDir,"obj")
    val incDir = new File(buildDir,"inc")
    val targetElf = new File(buildDir,"firmware.elf")
    val targetHex = new File(buildDir,"firmware.hex")
    val targetBin = new File(buildDir,"firmware.bin")
    val targetAsm = new File(buildDir,"firmware.asm")

    if ( cmdlOpts.targets.contains(Target.Rebuild) && buildDir.exists ) {
      FileUtils.deleteDirectory(objDir)
      FileUtils.deleteDirectory(incDir)
      buildDir.listFiles{_.isFile}.foreach{_.delete}
    } else if ( cmdlOpts.targets.contains(Target.Clean) && buildDir.exists ) {
      FileUtils.deleteDirectory(objDir)
      buildDir.listFiles{ f => f.isFile && f.getName != "script.xml" }.foreach{_.delete}
    }

    if ( !objDir.exists ) objDir.mkdirs()
    if ( !incDir.exists ) incDir.mkdirs()

    def expandEnv(s:String):Option[String] = "(\\%(\\w+)\\%)".r findFirstMatchIn s match {
      case Some(m) =>
        if ( sys.env.contains(m.group(2)) )
          expandEnv(s.replace(m.group(1),sys.env(m.group(2))))
        else
          None
      case None => Some(s)
    }

    val components = boardPragmas.foldLeft( Map[String,Component]() ) {
      (components,prag) => prag match {
        case UccmHome(tag,path) => components + (tag -> (components.get(tag) match {
          case Some(c) => c.copy(home = Some(path))
          case None => Component(home = Some(path))
        }))
        case UccmInfo(tag,text) => components + (tag -> (components.get(tag) match {
          case Some(c) => c.copy(info = Some(text))
          case None => Component(info = Some(text))
        }))
        case UccmDownload(tag,url) => components + (tag -> (components.get(tag) match {
          case Some(c) => c.copy(download = Some(url))
          case None => Component(download = Some(url))
        }))
        case _ => components
      }
    } +
      ("@inc" -> Component(home = Some(incDir.getPath)),
      "@obj" -> Component(home = Some(objDir.getPath)),
      "@build" -> Component(home = Some(buildDir.getPath)),
      "@src" -> Component(home = Some(".")))

    def dirExists(s:String):Boolean = { val f = new File(s); f.exists && f.isDirectory }

    case class Alias(name:String,expand:String)

    val aliases = boardPragmas.foldLeft( Map[String,String]("UCCM"->uccmHome.getCanonicalPath) ) {
      (aliases,prag) => prag match {
        case UccmAlias(tag,path) => aliases + (tag -> (aliases.get(tag) match {
          case None => path
          case Some(s) =>
            panic("aliases can't be rewrited");s
        }))
        case _ => aliases
      }
    }

    def expandHome(s:String):String = "(\\[([@\\w\\+\\.]+)\\])".r findFirstMatchIn s match {
      case None => s
      case Some(m) => components.get(m.group(2)) match {
        case Some(Component(None, info, Some(url))) =>
          panic(s"looks like required to download ${m.group(2)} from $url");s
        case Some(Component(Some(home), _, u)) =>
          expandEnv(home) match {
            case Some(path) if dirExists(path) => expandHome(s.replace(m.group(1),path))
            case Some(path) =>
              panic(s"${m.group(2)} home '$home' expands to nonexistant path '$path'");s
            case None => u match {
              case None =>
                panic(s"looks like ${m.group(2)} home '$home' uses undefined environment variable");s
              case Some(url) =>
                panic(s"looks like required to download ${m.group(2)} from $url");s
            }
          }
        case _ =>
          panic(s"unknown component ${m.group(2)}, could not expand compenent home");s
      }
    }

    def expandAlias(s:String):String = "(\\{([@\\w]+)\\})".r findFirstMatchIn s match {
      case None => expandHome(s)
      case Some(m) => aliases.get(m.group(2)) match {
        case None =>
          panic(s"unknown alias '${m.group(2)}' is used");s
        case Some(e) =>
          val f = new File(buildDir, m.group(2))
          val expand = expandHome(e)
          verbose(s"$f => $expand")
          if (!f.exists && new File(expand).isDirectory) {
            val cmdl = "cmd /c mklink /J \"" + f.getAbsolutePath.replace("/", "\\") + "\" \"" + expand.replace("/", "\\") + "\""
            println(cmdl)
            cmdl.!
          }
          expandAlias(s.replace(m.group(1),f.getPath))
      }
    }

    val xcflags = boardPragmas.foldLeft( s"-I{UCCM}" :: cmdlOpts.cflags ) {
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
    }.map{expandAlias}.reverse

    val buildScriptFile = new File(buildDir,"script.xml")
    if ( !buildScriptFile.exists || cmdlOpts.targets.contains(Target.Rebuild) )
      println(s"uccm is using ${Compiler.stringify(targetCompiler)} compiler")

    def extractFromPreprocessor : BuildScript = {
      val cc = expandHome(Compiler.ccPath(targetCompiler))
      val tempFile = File.createTempFile("uccm",s"-${Compiler.stringify(targetCompiler)}.i")
      val mainFilePath = mainFile.getPath
      val optSelector = cmdlOpts.buildConfig match {
        case BuildConfig.Release => " -D_RELEASE "
        case BuildConfig.Debug => " -D_DEBUG "
      }

      val gccPreprocCmdline = quote(cc) +
        " -E " +
        optSelector +
        xcflags.mkString(" ") +
        " " + mainFilePath

      println(s"preprocessing main C-file ...")
      verbose(gccPreprocCmdline)
      if ( 0 != (gccPreprocCmdline #> tempFile).! )
        panic("failed to preprocess main C-file")

      Pragmas.extractFromTempFile(tempFile).foldLeft(
        BuildScript(targetCompiler,targetDebugger,cmdlOpts.buildConfig,
          s"-I{UCCM}" :: optSelector :: cmdlOpts.cflags,
          List(mainFilePath))) {
        (bs,prag) => prag match {
          case UccmXcflags(x,value) => Compiler.fromString(x) match {
            case Some(`targetCompiler`) => bs.copy(cflags = value :: bs.cflags)
            case None if x == "*" => bs.copy(cflags = value :: bs.cflags)
            case _ => bs
          }
          case UccmCflags(value) => bs.copy(cflags = value :: bs.cflags)
          case UccmLdflags(value) => bs.copy(ldflags = value :: bs.ldflags)
          case UccmAsflags(value) => bs.copy(asflags = value :: bs.asflags)
          case UccmBoard(`targetBoard`,value) => bs.copy(cflags = value :: bs.cflags)
          case UccmBoard("*",value) => bs.copy(cflags = value :: bs.cflags)
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
          cflags = bs.cflags.map{expandAlias}.reverse,
          ldflags = bs.ldflags.map{expandAlias}.reverse,
          asflags = bs.asflags.map{expandAlias}.reverse,
          sources = bs.begin.map{expandAlias}.reverse ++
                    bs.sources.map{expandAlias}.reverse ++
                    bs.end.map{expandAlias},
          modules = bs.modules.map{expandAlias}.reverse,
          libraries = bs.libraries.map{expandAlias}.reverse
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
    else
      println(s"uccm is using ${Compiler.stringify(buildScript.ccTool)} compiler")

    if ( cmdlOpts.targets.contains(Target.Rebuild) )
      buildScript.generated.foreach {
        case ( fname, content ) =>
          val text = content.replace("\\n","\n").replace("\\t","\t")
          val wr = new FileWriter(new File(incDir,fname),true)
          wr.write(text)
          wr.close()
      }

    def compile(ls:List[String],source:String):List[String] = {
      val cc = expandHome(Compiler.ccPath(buildScript.ccTool))
      val asm:Option[String] = Compiler.asmPath(buildScript.ccTool) match {
        case Some(x) => Some(expandHome(x))
        case _ => None
      }
      val srcFile = new File(source)
      val objFileName = srcFile.getName + ".obj"
      val objFile = new File(objDir,objFileName)
      if ( cmdlOpts.targets.contains(Target.Rebuild) ||
        !objFile.exists || FileUtils.isFileOlder(objFile,srcFile) ) {
        println(s"compiling ${srcFile.getName} ...")
        val cmdline: String =
          if ( srcFile.getPath.toLowerCase.endsWith(".s") && asm.isDefined )
            (List(quote(asm.get)) ++
              buildScript.asflags ++
              List(quote(srcFile.getPath), "-o", objFile.getPath)).mkString(" ")
          else
            (List(quote(cc), "-c ") ++
              buildScript.cflags ++
              List(quote(srcFile.getPath), "-o", objFile.getPath)).mkString(" ")
        verbose(cmdline)
        if (0 != cmdline.!)
          panic(s"failed to compile ${srcFile.getName}")
      }
      objFile.getName :: ls
    }

    if ( cmdlOpts.targets.contains(Target.Qte) )
      QtProj.generate(mainFile,buildScript,buildDir,expandHome,verbose)

    else if ( cmdlOpts.targets.contains(Target.Build) ) {

      val objects = buildScript.sources.foldLeft(List[String]()) {
        compile
      }.reverse

      val modules = buildScript.modules.foldLeft(List[String]()) {
        compile
      }.reverse

      val ld = expandHome(Compiler.ldPath(buildScript.ccTool))
      val objFiles = (objects ++ modules).map { fn => new File(objDir, fn) }

      val haveToRelink = cmdlOpts.targets.contains(Target.Rebuild) ||
        !targetElf.exists ||
        objFiles.foldLeft(false) { (f, obj) => f || FileUtils.isFileOlder(targetElf, obj) }

      if (haveToRelink) {
        println("linking ...")
        List(targetBin, targetHex, targetElf).foreach { f => if (f.exists) f.delete }
        val gccCmdline = (List(quote(ld)) ++ buildScript.ldflags ++ objFiles ++
          List("-o", targetElf.getPath)).mkString(" ")
        verbose(gccCmdline)
        if (0 != gccCmdline.!)
          panic(s"failed to link ${targetElf.getName}")
        val toHexCmdl = expandHome(Compiler.elfToHexCmdl(buildScript.ccTool, targetElf, targetHex))
        verbose(toHexCmdl)
        if (0 != toHexCmdl.!)
          panic(s"failed to generate ${targetHex.getName}")
        val toBinCmdl = expandHome(Compiler.elfToBinCmdl(buildScript.ccTool, targetElf, targetBin))
        verbose(toBinCmdl)
        if (0 != toBinCmdl.!)
          panic(s"failed to generate ${targetBin.getName}")
        if ( buildScript.ccTool == Compiler.GCC ) {
          val cmdl = expandHome(Compiler.odmpPath(buildScript.ccTool).get) + " -d -S " + targetElf.getPath
          verbose(cmdl)
          if ( 0 != (cmdl #> targetAsm).! )
            panic(s"failed to generate ${targetAsm.getPath}")
        }
      }

      if ( cmdlOpts.targets.contains(Target.Erase) )
        Debugger.erase(buildScript.dbgTool,verbose) match {
          case Failure(f) =>
            panic(f.getMessage)
          case _ =>
        }

      if ( cmdlOpts.targets.contains(Target.Upload) )
        Debugger.upload(buildScript.dbgTool,targetHex,verbose,
          cmdlOpts.targets.contains(Target.Reset)) match {
          case Failure(f) =>
            panic(f.getMessage)
          case _ =>
        }
      else if ( cmdlOpts.targets.contains(Target.Reset) )
        Debugger.reset(buildScript.dbgTool,verbose) match {
          case Failure(f) =>
            panic(f.getMessage)
          case _ =>
        }

      if ( cmdlOpts.targets.contains(Target.Connect) )
        Debugger.connect(buildScript.dbgTool,verbose) match {
          case Failure(f) =>
            panic(f.getMessage)
          case _ =>
        }

    }
    println("succeeded")
  }

  def preprocessUccmBoardFiles(targetBoard:String,uccmHome:File) : List[UccmPragma] = {
    val boardDir = new File(uccmHome,"uccm/board")
    val localBoardDir = new File(getCurrentDirectory,"board")

    def f(s:Stream[File]) : Stream[List[UccmPragma]] =  s match {
      case xs #:: t => Pragmas.extractFrom(xs).toList #:: f(t)
      case _ => Stream.empty
    }

    val localBoardFiles : List[File] =
      if ( localBoardDir.exists && localBoardDir.isDirectory )
        localBoardDir.listFiles.toList
      else
        Nil

    val boardPragmas: Option[List[UccmPragma]] = {
      if ( boardDir.exists && boardDir.isDirectory )
        f((localBoardFiles ++ boardDir.listFiles.toList).toStream)
      else
        Stream.empty
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
}