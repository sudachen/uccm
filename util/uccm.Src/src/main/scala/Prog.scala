package com.sudachen.uccm

import java.io.{File, FileWriter, PrintWriter}

import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}
import sys.process.{ProcessLogger, _}
import scala.io.Source
import scala.sys.process
import scala.util.matching.Regex

object Prog {

  object Target extends Enumeration {
    val
    Clean, Reconfig, Build, ListBoards,
    Softdevice, Erase, Program, Connect, Reset,
    Qte, QteStart, RttView, JSTlink
    = Value
  }

  case class CmdlOptions(buildConfig: BuildConfig.Value = BuildConfig.Debug,
                         targets:  Set[Target.Value] = Set(),
                         board:    Option[String] = None,
                         compiler: Option[Compiler.Value] = None,
                         debugger: Option[Debugger.Value] = None,
                         mainFile: Option[File] = None,
                         verbose:  Boolean = false,
                         color:  Boolean = false,
                         cflags:  List[String] = Nil,
                         softDevice: Option[String] = None,
                         newMain: Boolean = false,
                         yes: Boolean = false,
                         devOpts: Boolean = true)

  def main(argv: Array[String]): Unit = {

    val cmdlParser = new scopt.OptionParser[CmdlOptions]("uccm") {
      head("Alexey Sudachen' uC cortex-m build manager, goo.gl/mKjRQ8")
      help("help").
        text("show this help and exit")

      override def showUsageOnError = true

      opt[Unit]('v',"verbose").
        action( (_,c) => c.copy(verbose = true)).
        text("verbose output")

      opt[Unit]('c',"color").
        action( (_,c) => c.copy(color = true)).
        text("colorize output")

      opt[Unit]('y',"yes").
        action( (_,c) => c.copy(yes = true)).
        text("enable required actions like download and install software")

      opt[Unit]("no-dev").
        action( (_,c) => c.copy(devOpts = false)).
        text("do not use development uccm version and disable local imports")

      opt[Unit]('n',"new-main").
        action( (_,c) => c.copy(newMain = true)).
        text("create new main.c file if no one exists")

      opt[Unit]("build").
        action( (_,c) => c.copy(targets = c.targets + Target.Build)).
        text("build project (by default)")

      opt[Unit]("no-build").
        action( (_,c) => c.copy(targets = c.targets - Target.Build)).
        text("skip project build action")

      opt[Unit]("clean").
        action( (_,c) => c.copy(targets = c.targets + Target.Clean)).
        text("remove intermediate files and target firmware")

      opt[Unit]("program-softdevice").
        action( (_,c) => c.copy(targets = c.targets + Target.Softdevice)).
        text("erase all uC memory and reprogram softdevice if one defined")

      opt[Unit]("erase").
        action( (_,c) => c.copy(targets = c.targets + Target.Erase)).
        text("erase uC memory")

      opt[Unit]("program").
        action( (_,c) => c.copy(targets = c.targets + Target.Program + Target.Build)).
        text("reprogram uC flash memory")

      opt[Unit]('r',"reset").
        action( (_,c) => c.copy(targets = c.targets + Target.Reset)).
        text("reset uC")

      opt[Unit]("connect").
        action( (_,c) => c.copy(targets = c.targets + Target.Connect)).
        text("try connect to uC")

      opt[String]('b',"board").
        action( (x,c) => c.copy(board = Some(x))).
        text("build for board")

      opt[Unit]('l',"list-boards").
        action( (x,c) => c.copy(targets = c.targets + Target.ListBoards)).
        text("list supported boards")

      opt[Unit]('j',"project").
        action( (_,c) => c.copy(targets = c.targets + Target.Qte)).
        text("update project only")

      opt[Unit]("edit").
        action( (_,c) => c.copy(targets = c.targets + Target.Qte + Target.QteStart)).
        text("update project and start code editor")

      //opt[Unit]("debug").
      //  action( (_,c) => c.copy()).
      //  text("connect debugger to uC, may be used with --reset")

      opt[Unit]("rtt-view").
        action( (_,c) => c.copy(targets = c.targets + Target.RttView)).
        text("start SEGGER jLinkRTTView terminal connected to uC")

      opt[Unit]("jlink-stlink").
        action( (_,c) => c.copy(targets = Set(Target.JSTlink))).
        text("start SEGGER STLinkReflash.exe utility and exit")

      opt[Unit]("reconfig").
        action( (_,c) => c.copy(targets = c.targets + Target.Reconfig)).
        text("reconfigure only")

      opt[Unit]("rebuild").
        action( (_,c) => c.copy(targets = c.targets + Target.Reconfig + Target.Build)).
        text("reconfigure and do clean build")

      opt[Unit]("release").
        action( (x,c) => c.copy(buildConfig = BuildConfig.Release) ).
        text("[on rebuild/reconfig] configure for release build")

      opt[Unit]("debug").
        action( (x,c) => c.copy(buildConfig = BuildConfig.Debug) ).
        text("[on rebuild/reconfig] configure for debug build")

      opt[String]('D',"define").
        action( (x,c) => c.copy(cflags = ("-D"+x)::c.cflags)).
        text("[on rebuild/reconfig] add macro definition to compiler cflags")

      opt[Unit]("gcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.GCC))).
        text("[on rebuild/reconfig] use ARM-NONE-EABI GNU C compiler")

      opt[Unit]("armcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.ARMCC))).
        text("[on rebuild/reconfig] use KeilV5 armcc compiler")

      opt[Unit]("armcc-microlib").
        action( (_,c) => c.copy(compiler = Some(Compiler.ARMCC),cflags = "-DUSE_MICROLIB"::c.cflags)).
        text("[on rebuild/reconfig] use KeilV5 armcc compiler with C-microlib")

      opt[Unit]("stlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.STLINK))).
        text("[on rebuild/reconfig] use STM ST-Link debugger/programmer")

      opt[Unit]("jlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.JLINK))).
        text("[on rebuild/reconfig] use SEGGER J-Link debugger/programmer")

      opt[Unit]("nrfjprog").
        action( (_,c) => c.copy(debugger = Some(Debugger.NRFJPROG))).
        text("[on rebuild/reconfig] use Nordic nrfjprog tool (requires J-Link)")

      opt[Unit]("raw").
        action( (_,c) => c.copy(softDevice = Some("RAW"))).
        text("[on rebuild/reconfig] use no softdevice")

      opt[Unit]("ble").
        action( (_,c) => c.copy(softDevice = Some("BLE"))).
        text("[on rebuild/reconfig] use BLE softdevice aka S130/S132")

      opt[String]("softdevice").
        action( (x,c) => c.copy(softDevice = Some(x.toUpperCase))).
        text("[on rebuild/reconfig] use specific softdevice")

      arg[File]("main.c").optional().
        action( (x,c) => c.copy(mainFile = Some(x))).
        text("firmware main.c file")
    }

    cmdlParser.parse(argv, CmdlOptions()) match {
      case Some(cmdlOpts) =>
        if ( cmdlOpts.targets.isEmpty ) {
          cmdlParser.showUsage()
          System.exit(1)
        }
        act(cmdlOpts)
      case None =>
    }
  }

  def getCurrentDirectory : String = new File(".").getCanonicalPath

  def expandEnv(s:String):Option[String] = "(\\%([\\|\\w]+)\\%)".r findFirstMatchIn s match {
    case Some(m) =>
      m.group(2).split('|').toList.dropWhile{x => !sys.env.contains(x)} match {
        case e::_ => expandEnv(s.replace(m.group(1),sys.env(e)))
        case Nil => None
      }
    case None => Some(s)
  }

  def expandEnv(s:Option[String]):Option[String] = s match {
    case None => None
    case Some(ss) => expandEnv(ss)
  }

  def act(cmdlOpts: CmdlOptions) : Unit = {

    BuildConsole.useColors = cmdlOpts.color
    BuildConsole.beVerbose = cmdlOpts.verbose
    BuildScript.enableDevOpts = cmdlOpts.devOpts

    val panic: String => Unit = BuildConsole.panic
    val info: String => Unit = BuildConsole.info
    val verbose: String => Unit = BuildConsole.verbose
    val verboseInfo: String => Unit = BuildConsole.verboseInfo

    val mainFile :File = cmdlOpts.mainFile match {
      case Some(f) => new File(f.getName)
      case None => new File("./main.c")
    }

    val uccmHome = BuildScript.uccmDirectoryFile

    if ( cmdlOpts.targets.contains(Target.ListBoards) ) {
      listBoards(uccmHome).sorted.foreach {
        println
      }
      System.exit(0)
    }

    val projectDir = mainFile.getParentFile

    if ( cmdlOpts.targets.contains(Target.JSTlink) ) {
      val stlr = "STLinkReflash.exe"

      if (Components.dflt.getComponentHome("jstlink").isEmpty)
        if ( cmdlOpts.yes ) {
          info(s"getting $stlr now")
          if (!Components.dflt.acquireComponent("jstlink"))
            panic(s"failed to download $stlr")
        } else {
          panic(s"looks like it's required to download $stlr, restart with -y")
        }

      val homeFile:File = expandEnv(Components.dflt.getComponentHome("jstlink")) match {
        case Some(path) if new File(path).exists => new File(path)
        case Some(path) =>
          panic(s"homedir of $stlr expands to nonexistant path '$path'"); null
        case None =>
          panic(s"looks like homedir of $stlr uses undefined environment variable"); null
      }

      //Util.winExePath(new File(homeFile,stlr).getAbsolutePath).!
      System.exit(3)
    }

    if ( !mainFile.exists && !cmdlOpts.newMain )
      panic("main file \"" + mainFile.getCanonicalPath + "\" does not exist")

    val mainPragmas = if (mainFile.exists) Pragma.extractFrom(mainFile).toList else Nil
    val targetBoard = cmdlOpts.board.getOrElse{ mainPragmas.foldLeft( Option[String](null) ) {
      ( dflts, prag ) => prag match {
        case Pragma.Default(tag,value) => tag match {
          case "board" => Some(value)
        }
        case _ => dflts
    }} match {
      case None =>
        info("board is not specified");""
      case Some(boardName) => boardName
    }}

    val boardPragmas : List[Pragma] = preprocessUccmBoardFiles(targetBoard,uccmHome) match {
      case Nil =>
        panic(s"unknown board $targetBoard"); Nil
      case lst => lst ++ mainPragmas
    }

    info(s"uccm is working now for board $targetBoard")

    val buildDir = new File(".",s"~$targetBoard")
    val buildScriptFile = new File(buildDir,"script.xml")

    BuildScript.buildDirFile = buildDir

    val targets =
      if (!buildDir.exists || !buildScriptFile.exists)
        cmdlOpts.targets + Target.Reconfig
      else
        cmdlOpts.targets

    if ( !buildDir.exists ) buildDir.mkdirs()

    val objDir = new File(buildDir,"obj")
    val incDir = new File(buildDir,"inc")
    val targetElf = new File(buildDir,"firmware.elf")
    val targetHex = new File(buildDir,"firmware.hex")
    val targetBin = new File(buildDir,"firmware.bin")
    val targetAsm = new File(buildDir,"firmware.asm")

    if ( targets.contains(Target.Reconfig) ) {
      if ( incDir.exists )
        incDir.listFiles.filter(_.getName.startsWith("~")).foreach{_.listFiles().foreach{_.delete()}}
      FileUtils.deleteDirectory(objDir)
      FileUtils.deleteDirectory(incDir)
      buildDir.listFiles.filter{_.isFile}.foreach{_.delete}
    } else if ( targets.contains(Target.Clean) ) {
      FileUtils.deleteDirectory(objDir)
      buildDir.listFiles.filter{ f => f.isFile && f.getName != "script.xml" }.foreach{_.delete}
    }

    if ( !objDir.exists ) objDir.mkdirs()
    if ( !incDir.exists ) incDir.mkdirs()

    def dirExists(s:String):Boolean = { val f = new File(s); f.exists && f.isDirectory }

    case class Alias(name:String,expand:String)

    val aliases = boardPragmas.foldLeft( Map[String,String]("UCCM"->uccmHome.getCanonicalPath) ) {
      (aliases,prag) => prag match {
        case Pragma.Alias(tag,path) => aliases + (tag -> (aliases.get(tag) match {
          case None => path
          case Some(s) =>
            panic("aliases can't be rewrited");s
        }))
        case _ => aliases
      }
    }

    def expandHome(s:String):String = "(\\[([@\\w\\+\\.]+)\\])".r findFirstMatchIn s match {
      case None => s
      case Some(m) if m.group(2).startsWith("@") => expandHome(s.replace(m.group(1), m.group(2) match {
        case "@inc" => incDir.getPath
        case "@obj" => objDir.getPath
        case "@build" => buildDir.getPath
        case "@src" => "."
        case _ =>
          panic(s"unknown component ${m.group(2)}, could not expand compenent home");""
      }))

      case Some(m) =>
        val comp = m.group(2)
        if (Components.dflt.getComponentHome(comp).isEmpty)
          if ( cmdlOpts.yes ) {
            info(s"getting $comp now")
            if (!Components.dflt.acquireComponent(comp))
              panic(s"failed to download $comp")
          } else {
            panic(s"looks like it's required to download $comp, restart with -y")
          }
        val home = Components.dflt.getComponentHome(comp).get
        expandEnv(home) match {
          case Some(path) if dirExists(path) => expandHome(s.replace(m.group(1),path))
          case Some(path) =>
            panic(s"$comp home '$home' expands to nonexistant path '$path'");s
          case None =>
            panic(s"looks like $comp home '$home' uses undefined environment variable");s
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
          if (!f.exists && dirExists(expand)) {
            info(s"$f => $expand")
            Try {
              Util.mlink(new File(expand),f)
            } match {
              case Success(ecode) =>
                if ( ecode != 0 )
                  panic("could not create simbolic link")
              case Failure(ee) =>
                BuildConsole.panicBt(ee.getMessage,ee.getStackTrace)
            }
          }
          expandAlias(s.replace(m.group(1),f.getPath))
      }
    }

    val softDeviceMap = boardPragmas.foldLeft(Map[String, String]()) {
      (vw, prag) =>
        prag match {
          case Pragma.SoftDevice(tag, pathToHex) => vw + (tag -> pathToHex)
          case _ => vw
        }
    }

    val targetSoftDevice = boardPragmas.foldLeft(cmdlOpts.softDevice.getOrElse(
      boardPragmas.foldLeft( Option[String](null) ) {
        (df,prag) => prag match {
          case Pragma.Default("softdevice",tag) => Some(tag)
          case _ => df
        }
      } match {
        case None => "RAW"
        case Some(name) => name
      })) {
        (n, prag) =>
          prag match {
            case Pragma.SoftDeviceAls(`n`, alias) => alias
            case _ => n
          }
        }

    if ( targetSoftDevice != "RAW" && !softDeviceMap.contains(targetSoftDevice) )
      panic(s"could not use unknown softdevice $targetSoftDevice")

    val targetCompiler: Compiler.Value = cmdlOpts.compiler match {
      case Some(cc) => cc
      case None => boardPragmas.foldLeft( Compiler.GCC ) {
        (cc, prag) => prag match {
          case Pragma.Default("compiler",value) => Compiler.fromString(value).get
          case _ => cc
        }
      }
    }

    if ( !Compiler.exists(targetCompiler) ) {
      if (!cmdlOpts.yes)
        panic(s"looks like it's required to download $targetCompiler, restart with -y")
      else {
        if (!Compiler.install(targetCompiler))
          panic(s"failed to download $targetCompiler")
      }
    }

    if ( targets.contains(Target.Qte) || targets.contains(Target.QteStart) ) {
      if ( QtProj.isRequiredToInstall )
        if (!cmdlOpts.yes)
          panic(s"looks like it's required to install QtCreator, restart with -y")
        else {
          if (!QtProj.install())
            panic("failed to install")
        }
    }

    val xcflags = boardPragmas.foldLeft( s"-I{UCCM}" :: cmdlOpts.cflags ) {
      (xcflags,prag) => prag match {
        case Pragma.Xcflags(`targetSoftDevice`,value) => value :: xcflags
        case Pragma.Xcflags("*",value) => value :: xcflags
        case Pragma.Xcflags(x,value) => Compiler.fromString(x) match {
          case Some(`targetCompiler`) => value :: xcflags
          case _ => xcflags
        }
        case Pragma.Board(`targetBoard`,value) => value :: xcflags
        case Pragma.Board("*",value) => value :: xcflags
        case _ => xcflags
      }
    }.map{expandAlias}.reverse

    if ( targets.contains(Target.Reconfig) )
      info(s"uccm is using ${Compiler.stringify(targetCompiler)} compiler")

    if ( !mainFile.exists ){
      val wr = new PrintWriter(mainFile)
      wr.println(s"#pragma uccm default(board)= $targetBoard")
      wr.println("#pragma uccm let(HEAP_SIZE)= 0")
      if ( cmdlOpts.softDevice.isDefined)
        wr.println(s"#pragma uccm default(softdevice)= ${cmdlOpts.softDevice.isDefined}")
      if ( cmdlOpts.compiler.isDefined )
        wr.println(s"#pragma uccm default(compiler)= ${Compiler.stringify(cmdlOpts.compiler.get)}")
      if ( cmdlOpts.debugger.isDefined )
        wr.println(s"#pragma uccm default(debugger)= ${Debugger.stringify(cmdlOpts.debugger.get)}")
      wr.println("")
      wr.println("#include <uccm/board.h>")
      wr.println("")
      wr.println("void main()")
      wr.println("{")
      wr.println("    ucSetup_Board();")
      wr.println("    ucSetOn_BoardLED(0);")
      wr.println("    for(;;) __NOP();")
      wr.println("}")
      wr.close()
    }

    val preTargetDebugger =
      if ( cmdlOpts.debugger.isDefined ) cmdlOpts.debugger
      else boardPragmas.foldLeft( Option.empty[Debugger.Value] ) {
        (opt,prag) => prag match {
          case Pragma.Default("debugger",name) => Debugger.fromString(name)
          case _ => opt
        }
      }

    if ( targets.contains(Target.Reconfig) ) {
      if (preTargetDebugger.isDefined) {
        info(s"uccm is using ${Debugger.stringify(preTargetDebugger.get)} programmer")
        if (Debugger.isRequiredToInstallSoftware(preTargetDebugger.get)) {
          if (!cmdlOpts.yes)
            panic(s"looks like required to download and install ${Debugger.softPakName(preTargetDebugger.get)}, restart with -y")
          else if (!Debugger.install(preTargetDebugger.get))
            panic(s"failed to install ${Debugger.stringify(preTargetDebugger.get)}")
        }
      } else
        info(s"uccm is not using any programmer")
    }

    if ( targets.contains(Target.RttView) ) {
      if ( Debugger.isRequiredToInstallSoftware(Debugger.JLINK) ) {
        if ( !cmdlOpts.yes )
          panic(s"looks like required to download and install SEGGER j-Link utilities, restart with -y")
        else if (!Debugger.install(Debugger.JLINK))
          panic("failed to install SEGGER j-Link utilities")
      }
    }

    def extractFromPreprocessor : BuildScript = {

      val cc = Compiler.ccPath(targetCompiler)
      val tempFile = File.createTempFile("uccm",s"-${Compiler.stringify(targetCompiler)}.i")
      tempFile.deleteOnExit()
      val mainFilePath = mainFile.getPath
      val optSelector = cmdlOpts.buildConfig match {
        case BuildConfig.Release => " -D_RELEASE "
        case BuildConfig.Debug => " -D_DEBUG "
      }

      val gccPreprocCmdline = cc +
        " -E " +
        optSelector +
        xcflags.mkString(" ") +
        " " + mainFilePath

      info(s"preprocessing main C-file ...")
      verbose(gccPreprocCmdline)
      if ( 0 != (gccPreprocCmdline #> tempFile).! )
        panic("failed to preprocess main C-file")

      def expandVar(lets: Map[String,String])(s: String): String = "(\\{\\$([\\w]+)\\})".r findFirstMatchIn s match {
        case None => s
        case Some(m) => lets.get(m.group(2)) match {
          case None =>
            panic(s"unknown var '${m.group(2)}' is used");s
          case Some(value) =>
            expandVar(lets)(s.replace(m.group(1),value))
        }
      }

      val debuggerTag = preTargetDebugger match {
        case Some(kind) => Debugger.stringify(kind)
        case None => ";"
      }

      Pragma.extractFromTempFile(tempFile).foldLeft(
        BuildScript(targetBoard,targetCompiler,preTargetDebugger,cmdlOpts.buildConfig,
          s"-I{UCCM}" :: optSelector :: cmdlOpts.cflags,
          List(mainFilePath))) {
        (bs,prag) => prag match {
          case Pragma.Xcflags(`targetSoftDevice`,value) => bs.copy(cflags = value :: bs.cflags)
          case Pragma.Xcflags("*",value) => bs.copy(cflags = value :: bs.cflags)
          case Pragma.Xcflags(x,value) => Compiler.fromString(x) match {
            case Some(`targetCompiler`) => bs.copy(cflags = value :: bs.cflags)
            case _ => bs
          }
          case Pragma.Cflags(value) => bs.copy(cflags = value :: bs.cflags)
          case Pragma.Ldflags(value) => bs.copy(ldflags = value :: bs.ldflags)
          case Pragma.Asflags(value) => bs.copy(asflags = value :: bs.asflags)
          case Pragma.Board(`targetBoard`,value) => bs.copy(cflags = value :: bs.cflags)
          case Pragma.Board("*",value) => bs.copy(cflags = value :: bs.cflags)
          case Pragma.Require("module",value) => bs.copy(modules = value :: bs.modules)
          case Pragma.Require("source",value) => bs.copy(sources = value :: bs.sources)
          case Pragma.Require("lib",value) => bs.copy(libraries = value :: bs.libraries)
          case Pragma.Require("begin",value) => bs.copy(begin = value :: bs.begin)
          case Pragma.Require("end",value) => bs.copy(end = value :: bs.end)
          case Pragma.Append(tag,value) => bs.copy(generatedPart = (tag, (x : Any) => value) :: bs.generatedPart )
          case Pragma.AppendEx(tag,value) => bs.copy(generatedPart = (tag, (x : Map[String,String]) => expandVar(x)(expandAlias(value))) :: bs.generatedPart )
          case Pragma.Let(name,value) => bs.copy(lets = bs.lets + (name -> value) )
          case Pragma.LetIfNo(name,value) => if ( bs.lets.contains(name) ) bs else bs.copy(lets = bs.lets + (name -> value) )
          case Pragma.Debugger(`debuggerTag`,opt) => bs.copy(debuggerOpt = opt :: bs.debuggerOpt)
          case Pragma.Debugger("jrttview",opt) => bs.copy(jRttViewOpt = opt :: bs.jRttViewOpt)
          case Pragma.Copy(tag,value) => bs.copy(copyfile = bs.copyfile + (tag -> FileCopy(tag,value)))

          case Pragma.Replace(tag,value) =>
            val rr = value.drop(1).split(value.charAt(0))
            verbose(s"${rr(0)}=>${rr(1)}")
            val ff = (x:String,q:Map[String,String]) => new Regex(rr(0)).findFirstMatchIn(x) match {
              case Some(g) => g.before(1).toString + rr(1) + g.after(1).toString
              case None => x
            }
            val old = bs.copyfile(tag)
            bs.copy(copyfile = bs.copyfile + (tag -> old.copy(replace = ff :: old.replace) ) )

          case Pragma.ReplaceEx(tag,value) =>
            val rr = value.drop(1).split(value.charAt(0))
            verbose(s"${rr(0)}=>expandVar(expandAlias(${rr(1)}))")
            bs.copy(copyfile = bs.copyfile + (tag -> bs.copyfile(tag).copy(replace =
              ((x:String,q:Map[String,String]) => new Regex(rr(0)).findFirstMatchIn(x) match {
                case Some(g) => g.before(1).toString + expandVar(q)(expandAlias(rr(1))) + g.after(1).toString
                case None => x
              }) :: bs.copyfile(tag).replace )))

          case _ => bs
        }
      } match {
        case bs =>
          val ulets = bs.lets + ("FIRMWARE_FILE_HEX"->targetHex.getPath)
          bs.copy(
            softDevice = targetSoftDevice,
            generated = bs.generatedPart.map{t => (t._1,t._2(ulets))}.reverse,
            cflags = bs.cflags.map{expandAlias}.map{expandVar(ulets)}.reverse,
            ldflags = bs.ldflags.map{expandAlias}.map{expandVar(ulets)}.reverse,
            asflags = bs.asflags.map{expandAlias}.map{expandVar(ulets)}.reverse,
            sources = (bs.begin.reverse ++ bs.sources.reverse ++ bs.end).map{expandAlias}.map{expandVar(bs.lets)},
            modules = bs.modules.map{expandAlias}.map{expandVar(ulets)}.reverse,
            libraries = bs.libraries.map{expandAlias}.map{expandVar(ulets)}.reverse,
            debuggerOpt = bs.debuggerOpt.map{expandAlias}.map{expandVar(ulets)}.reverse,
            jRttViewOpt = bs.jRttViewOpt.map{expandAlias}.map{expandVar(ulets)}.reverse,
            copyfile = bs.copyfile.mapValues{ v => v.copy(replace = v.replace.reverse) },
            lets = ulets,
            generatedPart = Nil,
            begin = Nil,
            end = Nil
        )
      }
    }

    if ( targets.contains(Target.Reconfig) )
      Try { Import.importAll(mainFile) } match {
        case Success(is) =>
          is.imports.foreach {
            imp => Try {
              val userDir = new File(incDir, s"~${imp.ghUser}")
              if (!userDir.exists) userDir.mkdir()
              val targFile = new File(userDir, s"${imp.name}")
              info(s"${targFile.getPath} =>${imp.dirFile.getPath}")
              Util.mlink(imp.dirFile,targFile)
            } match {
              case Success(ecode) =>
                if ( ecode != 0 )
                  panic("could not create simbolic link to module")
              case Failure(e) =>
                BuildConsole.panicBt(e.getMessage,e.getStackTrace)
            }
          }
        case Failure(e) =>
          BuildConsole.panicBt(e.getMessage,e.getStackTrace)
      }

    val buildScript =
      if ( targets.contains(Target.Reconfig) )
        extractFromPreprocessor
      else
        BuildScript.fromXML(scala.xml.XML.loadFile(buildScriptFile))

    val targetDebugger = buildScript.debugger

    if ( targets.contains(Target.Reconfig) ) {

      scala.xml.XML.save(buildScriptFile.getCanonicalPath, buildScript.toXML)

    } else {

      info(s"uccm is using ${Compiler.stringify(buildScript.ccTool)} compiler")
      if ( !targets.contains(Target.Reconfig) ) {
        if (targetDebugger.isDefined)
          info(s"uccm is using ${Debugger.stringify(targetDebugger.get)} programmer")
        else
          info(s"uccm is not using any programmer")
      }

    }

    if ( buildScript.softDevice.toUpperCase == "RAW" )
      info(s"uccm is not using any softdevice")
    else
      info(s"uccm is using softdevice ${buildScript.softDevice}")

    info(s"uccm is configured for ${BuildConfig.stringify(buildScript.config)}")

    if ( targets.contains(Target.Reconfig) ) {
      info("using next values:")
      buildScript.lets.foreach {
        case (name, value) => info(s"  $name => $value")
      }
    }

    if ( targets.contains(Target.Reconfig) )
      buildScript.copyfile.foreach {
        case ( _, cpf ) =>
          info("copying with edit to "+cpf.to)
          def repl(s:String) = cpf.replace.foldLeft(s) { (s,r) => r(s,buildScript.lets) }
          Try{ Util.copyFileToDir(incDir, expandAlias(cpf.from), cpf.to, repl) } match {
            case Success(_) =>
            case Failure(e) => panic(e.getMessage)
          }
      }

    if ( targets.contains(Target.Reconfig) ) {
      buildScript.generated.foldRight(Map[String,List[String]]()) {
        (t,m) => t match { case (name, content) => m + (name -> (content :: m.getOrElse(name,Nil))) }
      }.foreach {
        case (fname, content) =>
          info("generating "+fname)
          val wr = new FileWriter(new File(incDir, fname), true)
          content.foreach { s => wr.write(s.replace("\\n", "\n").replace("\\t", "\t")) }
          wr.close()
      }
    }

    val depsFile = new File(buildDir,"depends.txt")
    def mkdeps() = {
      def rightSlash(s:String) = s.map{ case '\\' => '/' case x => x }
      def local(str:String) = rightSlash(str) match { case s => if (s.startsWith("./")) s.drop(2) else s }
      val cc = Compiler.ccPath(targetCompiler)
      val cmdl = cc + " -M " + buildScript.cflags.mkString(" ") + " " + mainFile.getPath
      val where = local(buildDir.getPath)
      val rx = ("("+where+"/inc/~?[/\\w\\.]+.h|"+where+"/UCCM/[/\\w\\.]+.h|\\s[\\w\\.\\+]+.h)").r
      if ( depsFile.exists ) depsFile.delete()
      info(s"resolving headers dependenses on ${mainFile.getName}")
      val wr = new FileWriter(depsFile, false)
      val pl = ProcessLogger(s =>
        rx.findAllMatchIn(rightSlash(s)).foreach {
          case rx(path) =>
            wr.write(Util.ns(path)+"\n")
          case _ =>
        },
        s => Unit)
      BuildConsole.verbose(cmdl)
      cmdl!pl
      wr.close()
    }

    if ( targets.contains(Target.Reconfig) ||
      !depsFile.exists ||
      FileUtils.isFileOlder(depsFile,mainFile) )
      mkdeps()

    def compile(lt:Long)(ls:List[String],source:String):List[String] = {
      val cc = Compiler.ccPath(buildScript.ccTool)
      val asm = Compiler.asmPath(buildScript.ccTool)
      val srcFile = new File(source)
      val objFileName = srcFile.getName + ".obj"
      val objFile = new File(objDir,objFileName)
      if ( targets.contains(Target.Reconfig) ||
        !objFile.exists || FileUtils.isFileOlder(objFile,srcFile) ||
        ( lt != 0  && objFile.lastModified() < lt ) ) {
        info(s"compiling ${srcFile.getName} ...")
        val cmdline: String =
          if ( srcFile.getPath.toLowerCase.endsWith(".s") && asm.isDefined )
            (asm.get ::
              (buildScript.asflags ++
              List(Util.quote(srcFile.getPath), "-o", objFile.getPath))).mkString(" ")
          else
            (List(cc, "-c ") ++
              buildScript.cflags ++
              List(Util.quote(srcFile.getPath), "-o", objFile.getPath)).mkString(" ")
        verbose(cmdline)
        if (0 != cmdline.!)
          panic(s"failed to compile ${srcFile.getName}")
      }
      objFile.getName :: ls
    }

    if ( targets.contains(Target.Qte)  )
      QtProj.generate(mainFile,buildScript,buildDir,expandHome)

    if ( targets.contains(Target.Build) ) {

      val deps = {
        val f = Source.fromFile(depsFile)
        try f.getLines.toList finally f.close()
      }

      val lt = deps.foldLeft(0L) {
        (t, f) => Math.max(new File(f).lastModified(), t)
      }

      val objects = buildScript.sources.foldLeft(List[String]()) {
        compile(lt)
      }.reverse

      val modules = buildScript.modules.foldLeft(List[String]()) {
        compile(0L)
      }.reverse

      val ld = Compiler.ldPath(buildScript.ccTool)
      val objFiles = (objects ++ modules).map { fn => new File(objDir, fn) }

      val haveToRelink = targets.contains(Target.Reconfig) ||
        !targetElf.exists ||
        objFiles.foldLeft(false) { (f, obj) => f || FileUtils.isFileOlder(targetElf, obj) }

      if (haveToRelink) {
        info("linking ...")
        List(targetBin, targetHex, targetElf).foreach { f => if (f.exists) f.delete }
        val gccCmdline = (ld :: (buildScript.ldflags ++ objFiles ++ List("-o", targetElf.getPath))).mkString(" ")
        verbose(gccCmdline)
        if (0 != gccCmdline.!)
          panic(s"failed to link ${targetElf.getName}")
        val toHexCmdl = Compiler.elfToHexCmdl(buildScript.ccTool, targetElf, targetHex)
        verbose(toHexCmdl)
        if (0 != toHexCmdl.!)
          panic(s"failed to generate ${targetHex.getName}")
        val toBinCmdl = Compiler.elfToBinCmdl(buildScript.ccTool, targetElf, targetBin)
        verbose(toBinCmdl)
        if (0 != toBinCmdl.!)
          panic(s"failed to generate ${targetBin.getName}")
        if (buildScript.ccTool == Compiler.GCC) {
          val cmdl = Compiler.odmpPath(buildScript.ccTool).get + " -d -S " + targetElf.getPath
          verbose(cmdl)
          if (0 != (cmdl #> targetAsm).!)
            panic(s"failed to generate ${targetAsm.getPath}")
        }
      }
    }

    if (targets.intersect(Set(Target.Softdevice, Target.Erase, Target.Program, Target.Reset, Target.Connect)).nonEmpty)
      if (targetDebugger.isEmpty)
        panic("programmer is not defined")
      else {
        val targetsOrder = List(Target.Softdevice, Target.Erase, Target.Program, Target.Reset, Target.Connect)

        val targetsSet =
          if (targets(Target.Program) && targets(Target.Reset))
            targets - Target.Reset
          else
            targets

        lazy val softDeviceHex: Option[File] = buildScript.softDevice match {
          case "RAW" => None
          case tag =>
            if (softDeviceMap.contains(tag)) {
              val fileName = expandAlias(softDeviceMap(tag))
              info(s"using softdevice '$fileName'")
              Some(new File(fileName))
            } else {
              panic(s"unknown softdevice $tag")
              None
            }
        }

        targetsOrder.filter {
          targetsSet(_)
        } foreach { t =>
          (t match {
            case Target.Erase =>
              Debugger.erase(buildScript.debugger.get, buildScript.debuggerOpt)
            case Target.Program =>
              Debugger.program(buildScript.debugger.get, targetHex,
                targets.contains(Target.Reset), buildScript.debuggerOpt)
            case Target.Reset =>
              Debugger.reset(buildScript.debugger.get, buildScript.debuggerOpt)
            case Target.Connect =>
              Debugger.connect(buildScript.debugger.get, buildScript.debuggerOpt)
            case Target.Softdevice =>
              Debugger.programSoftDevice(buildScript.debugger.get, buildScript.debuggerOpt, softDeviceHex)
          }) match {
            case Failure(f) =>
              panic(f.getMessage)
            case _ =>
          }
        }
      }

    info("succeeded")

    if ( targets.contains(Target.QteStart) ) Try {
      val project = Util.quote(new File(buildScript.boardName).getAbsolutePath + ".creator")
      val settings = Util.quote(QtProj.qteSettingsFile.getAbsolutePath)
      val mainC = Util.quote(mainFile.getPath)
      val cmd = List(QtProj.qteExe,"-settingspath",settings,project,mainC).mkString(" ")
      verbose(cmd)
      cmd.run()
    } match {
      case Success(_) => info("wait a little, code editor is starting ...")
      case Failure(e) => BuildConsole.error(e.getMessage)
    }

    if ( targets.contains(Target.RttView) ) {
      Debugger.jRttView(buildScript.jRttViewOpt) match {
        case Success(_) => info("wait a little, SEGGER JLinkRTTViewer is starting ...")
        case Failure(e) => BuildConsole.error(e.getMessage)
      }
    }

    System.exit(0)
  }

  def boardPragmas(uccmHome:File) : Stream[List[Pragma]] = {
    val boardDir = new File(uccmHome,"uccm/board")
    val localBoardDir = new File(getCurrentDirectory,"board")

    def f(s:Stream[File]) : Stream[List[Pragma]] =  s match {
      case xs #:: t => Pragma.extractFrom(xs).toList #:: f(t)
      case _ => Stream.empty
    }

    val localBoardFiles =
      if ( localBoardDir.exists && localBoardDir.isDirectory )
        localBoardDir.listFiles.toList
      else
        Nil

    if ( boardDir.exists && boardDir.isDirectory )
      f((localBoardFiles ++ boardDir.listFiles.toList).toStream)
    else
      Stream.empty
  }

  def preprocessUccmBoardFiles(targetBoard:String,uccmHome:File) : List[Pragma] = {
    val pragmas = boardPragmas(uccmHome).find {
      _.exists {
        case Pragma.Board(`targetBoard`,_) => true
        case _ => false
      }
    }
    if ( pragmas.nonEmpty )
      Pragma.extractFrom(new File(uccmHome,"uccm/uccm.h")).toList ++ pragmas.get
    else
      Nil
  }

  def listBoards(uccmHome:File) : List[String] = boardPragmas(uccmHome).
    foldLeft(Set[String]()){
      (s, l) => l.foldLeft(s){
        (ss, p) => p match {
            case Pragma.Board(tag,_) => ss + tag
            case _ => ss
        }
      }
    }.toList

}
