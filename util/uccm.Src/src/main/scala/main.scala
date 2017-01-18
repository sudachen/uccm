package com.sudachen.uccm

import java.io.{File, FileWriter, PrintWriter}
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success, Try}
import sys.process._
import scala.io.Source

object Target extends Enumeration {
  val Clean, Rebuild, Build, Softdevice, Erase, Program, Connect, Reset, Qte, QteStart = Value
}

case class CmdlOptions(buildConfig: BuildConfig.Value = BuildConfig.Release,
                       targets:  Set[Target.Value] = Set(Target.Build),
                       board:    Option[String] = None,
                       compiler: Option[Compiler.Value] = None,
                       debugger: Option[Debugger.Value] = None,
                       mainFile: Option[File] = None,
                       verbose:  Boolean = false,
                       color:  Boolean = false,
                       cflags:  List[String] = Nil,
                       softDevice: Option[String] = None,
                       newMain: Boolean = false,
                       yes: Boolean = false
                      )

object Prog {

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
        action( (_,c) => c.copy(targets = c.targets + Target.Clean - Target.Build)).
        text("remove intermediate files and target firmware")

      opt[Unit]("program-softdevice").
        action( (_,c) => c.copy(targets = c.targets + Target.Softdevice)).
        text("erase all uC memory and reprogram softdevice if one defined")

      opt[Unit]("erase").
        action( (_,c) => c.copy(targets = c.targets + Target.Erase)).
        text("erase uC memory")

      opt[Unit]("program").
        action( (_,c) => c.copy(targets = c.targets + Target.Program)).
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

      opt[Unit]('j',"project").
        action( (_,c) => c.copy(targets = c.targets + Target.Qte - Target.Build)).
        text("update project only")

      opt[Unit]("edit").
        action( (_,c) => c.copy(targets = c.targets + Target.Qte + Target.QteStart - Target.Build)).
        text("update project and start code editor")

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

      opt[Unit]("nrfjprog").
        action( (_,c) => c.copy(debugger = Some(Debugger.NRFJPROG))).
        text("[on rebuild] use Nordic nrfjprog tool (requires J-Link)")

      opt[Unit]("raw").
        action( (_,c) => c.copy(softDevice = Some("RAW"))).
        text("[on rebuild] use no softdevice")

      opt[Unit]("ble").
        action( (_,c) => c.copy(softDevice = Some("BLE"))).
        text("[on rebuild] use BLE softdevice aka S130/S132")

      opt[String]("softdevice").
        action( (x,c) => c.copy(softDevice = Some(x.toUpperCase))).
        text("[on rebuild] use specific softdevice")

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

  def act(cmdlOpts: CmdlOptions) : Unit = {

    BuildConsole.useColors = cmdlOpts.color
    BuildConsole.beVerbose = cmdlOpts.verbose

    val panic: String => Unit = BuildConsole.panic
    val info: String => Unit = BuildConsole.info
    val verbose: String => Unit = BuildConsole.verbose

    val mainFile :File = cmdlOpts.mainFile match {
      case Some(f) => new File(f.getName)
      case None => new File("./main.c")
    }

    val projectDir = mainFile.getParentFile

    if ( !mainFile.exists && !cmdlOpts.newMain )
      panic("main file \"" + mainFile.getCanonicalPath + "\" does not exist")

    val mainPragmas = if (mainFile.exists) Pragmas.extractFrom(mainFile).toList else Nil
    val targetBoard = cmdlOpts.board.getOrElse{ mainPragmas.foldLeft( Option[String](null) ) {
      ( dflts, prag ) => prag match {
        case UccmDefault(tag,value) => tag match {
          case "board" => Some(value)
        }
        case _ => dflts
    }} match {
      case None =>
        info("board is not specified");""
      case Some(boardName) => boardName
    }}

    val uccmHome = BuildScript.uccmDirectoryFile

    val boardPragmas : List[UccmPragma] = preprocessUccmBoardFiles(targetBoard,uccmHome) ++ mainPragmas match {
      case Nil =>
        panic(s"unknown board $targetBoard"); Nil
      case lst => lst
    }

    info(s"uccm is working now for board $targetBoard")

    val buildDir = new File(".",
      s"~$targetBoard"
    )

    val prepareBuildDir = !buildDir.exists
    if ( !buildDir.exists ) buildDir.mkdirs()
    val objDir = new File(buildDir,"obj")
    val incDir = new File(buildDir,"inc")
    val targetElf = new File(buildDir,"firmware.elf")
    val targetHex = new File(buildDir,"firmware.hex")
    val targetBin = new File(buildDir,"firmware.bin")
    val targetAsm = new File(buildDir,"firmware.asm")
    if ( cmdlOpts.targets.contains(Target.Rebuild) ) {
      FileUtils.deleteDirectory(objDir)
      FileUtils.deleteDirectory(incDir)
      buildDir.listFiles.filter{_.isFile}.foreach{_.delete}
    } else if ( cmdlOpts.targets.contains(Target.Clean) ) {
      FileUtils.deleteDirectory(objDir)
      buildDir.listFiles.filter{ f => f.isFile && f.getName != "script.xml" }.foreach{_.delete}
    }

    if ( !objDir.exists ) objDir.mkdirs()
    if ( !incDir.exists ) incDir.mkdirs()

    def expandEnv(s:String):Option[String] = "(\\%([\\|\\w]+)\\%)".r findFirstMatchIn s match {
      case Some(m) =>
        m.group(2).split('|').toList.dropWhile{x => !sys.env.contains(x)} match {
          case e::_ => expandEnv(s.replace(m.group(1),sys.env(e)))
          case Nil => None
        }
      case None => Some(s)
    }

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
            verbose(s"$f => $expand")
            val cmdl = "cmd /c mklink /J \"" + f.getAbsolutePath.replace("/", "\\") + "\" \"" + expand.replace("/", "\\") + "\""
            info(cmdl)
            cmdl.!
          }
          expandAlias(s.replace(m.group(1),f.getPath))
      }
    }

    val softDeviceMap = boardPragmas.foldLeft(Map[String, String]()) {
      (vw, prag) =>
        prag match {
          case UccmSoftDevice(tag, pathToHex) => vw + (tag -> pathToHex)
          case _ => vw
        }
    }

    val targetSoftDevice = boardPragmas.foldLeft(cmdlOpts.softDevice.getOrElse(
      boardPragmas.foldLeft( Option[String](null) ) {
        (df,prag) => prag match {
          case UccmDefault("softdevice",tag) => Some(tag)
          case _ => df
        }
      } match {
        case None => "RAW"
        case Some(name) => name
      })) {
        (n, prag) =>
          prag match {
            case UccmSoftDeviceAls(`n`, alias) => alias
            case _ => n
          }
        }

    if ( targetSoftDevice != "RAW" && !softDeviceMap.contains(targetSoftDevice) )
      panic(s"could not use unknown softdevice $targetSoftDevice")

    val targetCompiler: Compiler.Value = cmdlOpts.compiler match {
      case Some(cc) => cc
      case None => boardPragmas.foldLeft( Compiler.GCC ) {
        (cc, prag) => prag match {
          case UccmDefault("compiler",value) => Compiler.fromString(value).get
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

    if ( cmdlOpts.targets.contains(Target.Qte) || cmdlOpts.targets.contains(Target.QteStart) ) {
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
        case UccmXcflags(`targetSoftDevice`,value) => value :: xcflags
        case UccmXcflags("*",value) => value :: xcflags
        case UccmXcflags(x,value) => Compiler.fromString(x) match {
          case Some(`targetCompiler`) => value :: xcflags
          case _ => xcflags
        }
        case UccmBoard(`targetBoard`,value) => value :: xcflags
        case UccmBoard("*",value) => value :: xcflags
        case _ => xcflags
      }
    }.map{expandAlias}.reverse

    val buildScriptFile = new File(buildDir,"script.xml")
    if ( !buildScriptFile.exists || cmdlOpts.targets.contains(Target.Rebuild) )
      info(s"uccm is using ${Compiler.stringify(targetCompiler)} compiler")

    if ( !mainFile.exists ){
      val wr = new PrintWriter(mainFile)
      wr.println(s"#pragma uccm default(board)= $targetBoard")
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

      Pragmas.extractFromTempFile(tempFile).foldLeft(
        BuildScript(targetBoard,targetCompiler,None,cmdlOpts.buildConfig,
          s"-I{UCCM}" :: optSelector :: cmdlOpts.cflags,
          List(mainFilePath))) {
        (bs,prag) => prag match {
          case UccmXcflags(`targetSoftDevice`,value) => bs.copy(cflags = value :: bs.cflags)
          case UccmXcflags("*",value) => bs.copy(cflags = value :: bs.cflags)
          case UccmXcflags(x,value) => Compiler.fromString(x) match {
            case Some(`targetCompiler`) => bs.copy(cflags = value :: bs.cflags)
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
          case UccmAppendEx(tag,value) => bs.copy(generated = (tag,expandAlias(value)) :: bs.generated )
          case UccmDefault("debugger",value) => bs.copy( debugger = Debugger.fromString(value) )
          case _ => bs
        }
      } match {
        case bs => bs.copy(
          softDevice = targetSoftDevice,
          generated = bs.generated.reverse,
          cflags = bs.cflags.map{expandAlias}.reverse,
          ldflags = bs.ldflags.map{expandAlias}.reverse,
          asflags = bs.asflags.map{expandAlias}.reverse,
          sources = bs.begin.map{expandAlias}.reverse ++
                    bs.sources.map{expandAlias}.reverse ++
                    bs.end.map{expandAlias},
          modules = bs.modules.map{expandAlias}.reverse,
          libraries = bs.libraries.map{expandAlias}.reverse,
          debugger = if ( cmdlOpts.debugger.isDefined ) cmdlOpts.debugger else bs.debugger
        )
      }
    }

    val buildScriptExists = buildScriptFile.exists

    if ( !buildScriptExists || cmdlOpts.targets.contains(Target.Rebuild) )
      Try { Import.importAll(mainFile,ImportState(Import.loadCache())) } match {
        case Success(is) =>
          is.imports.foreach {
            imp => Try {
              val userDir = new File(incDir, s"${imp.ghUser}")
              if (!userDir.exists) userDir.mkdir()
              val fromWhere = imp.dirFile.getAbsolutePath.map { case '/' => '\\' case x => x }
              val toWhere = new File(incDir, s"${imp.ghUser}/${imp.name}").getAbsolutePath.map { case '/' => '\\' case x => x }
              val cmdl = List("cmd", "/c", "mklink", "/J", Util.quote(toWhere), Util.quote(fromWhere)).mkString(" ")
              verbose(cmdl)
              cmdl.!
            } match {
              case Success(ecode) =>
                if ( ecode != 0 )
                  panic("could not create simbolic link to module")
              case Failure(e) =>
                panic(e.getMessage)
            }
          }
        case Failure(e) => panic(e.getMessage)
      }

    val buildScript =
      if ( !buildScriptExists || cmdlOpts.targets.contains(Target.Rebuild) )
        extractFromPreprocessor
      else
        BuildScript.fromXML(scala.xml.XML.loadFile(buildScriptFile))

    if ( !buildScriptExists || cmdlOpts.targets.contains(Target.Rebuild) ) {
      scala.xml.XML.save(buildScriptFile.getCanonicalPath, buildScript.toXML)
    } else {
      info(s"uccm is using ${Compiler.stringify(buildScript.ccTool)} compiler")
    }

    info(s"uccm is using softdevice ${buildScript.softDevice}")

    val targetDebugger = if ( cmdlOpts.debugger.isDefined ) cmdlOpts.debugger else buildScript.debugger
    if ( targetDebugger.isDefined ) {
      info(s"uccm is using ${Debugger.stringify(targetDebugger.get)} debugger")
      if ( Debugger.isRequiredToInstallSoftware(targetDebugger.get) ) {
        if ( cmdlOpts.yes )
          Debugger.install(targetDebugger.get)
        else
          panic(s"looks like required to download and install ${Debugger.stringify(targetDebugger.get)} component, restart with -y")
      }
    } else
      info(s"uccm is not using any debugger")

    val dbgConnect:List[String] = targetDebugger match {
      case Some(debugger) =>
        val tag = Debugger.stringify(debugger)
        boardPragmas.foldLeft( List[String]() ) {
          (dbg, prag) => prag match {
            case UccmDebugger(`tag`,opts) => opts :: dbg
            case _ => dbg
          }}
      case None => Nil
    }

    if ( cmdlOpts.targets.contains(Target.Rebuild) || prepareBuildDir )
      buildScript.generated.foreach {
        case ( fname, content ) =>
          val text = content.replace("\\n","\n").replace("\\t","\t")
          val wr = new FileWriter(new File(incDir,fname),true)
          wr.write(text)
          wr.close()
      }

    val depsFile = new File(buildDir,"depends.txt")
    def mkdeps() = {
      def rightSlash(s:String) = s.map{ case '\\' => '/' case x => x }
      def local(str:String) = rightSlash(str) match { case s => if (s.startsWith("./")) s.drop(2) else s }
      val cc = Compiler.ccPath(targetCompiler)
      val cmdl = cc + " -M " + buildScript.cflags.mkString(" ") + " " + mainFile.getPath
      val where = local(buildDir.getPath)
      val rx = ("("+where+"/inc/[/\\w\\.]+.h|"+where+"/UCCM/[/\\w\\.]+.h|\\s[\\w\\.\\+]+.h)").r
      if ( depsFile.exists ) depsFile.delete()
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

    if ( cmdlOpts.targets.contains(Target.Rebuild) ||
      !depsFile.exists ||
      FileUtils.isFileOlder(depsFile,mainFile) )
      mkdeps()

    def compile(lt:Long)(ls:List[String],source:String):List[String] = {
      val cc = Compiler.ccPath(buildScript.ccTool)
      val asm = Compiler.asmPath(buildScript.ccTool)
      val srcFile = new File(source)
      val objFileName = srcFile.getName + ".obj"
      val objFile = new File(objDir,objFileName)
      if ( cmdlOpts.targets.contains(Target.Rebuild) ||
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

    if ( cmdlOpts.targets.contains(Target.Qte)  )
      QtProj.generate(mainFile,buildScript,buildDir,expandHome)

    if ( cmdlOpts.targets.contains(Target.Build) ) {

      val deps = {
        val f = Source.fromFile(depsFile)
        try f.getLines.toList finally f.close()
      }

      val lt = deps.foldLeft(0L) {
        (t, f) => Math.max(new File(f).lastModified(),t)
      }

      val objects = buildScript.sources.foldLeft(List[String]()) {
        compile(lt)
      }.reverse

      val modules = buildScript.modules.foldLeft(List[String]()) {
        compile(0L)
      }.reverse

      val ld = Compiler.ldPath(buildScript.ccTool)
      val objFiles = (objects ++ modules).map { fn => new File(objDir, fn) }

      val haveToRelink = cmdlOpts.targets.contains(Target.Rebuild) ||
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

      if (cmdlOpts.targets.intersect(Set(Target.Softdevice, Target.Erase, Target.Program, Target.Reset, Target.Connect)).nonEmpty)
        if (targetDebugger.isEmpty)
          panic("debuger is not defined")
        else {
          val targetsOrder = List(Target.Softdevice, Target.Erase, Target.Program, Target.Reset, Target.Connect)

          val targets =
            if (cmdlOpts.targets(Target.Program) && cmdlOpts.targets(Target.Reset))
              cmdlOpts.targets - Target.Reset
            else
              cmdlOpts.targets

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
            targets(_)
          } foreach { t =>
            (t match {
              case Target.Erase =>
                Debugger.erase(buildScript.debugger.get, dbgConnect)
              case Target.Program =>
                Debugger.program(buildScript.debugger.get, targetHex,
                  cmdlOpts.targets.contains(Target.Reset), dbgConnect)
              case Target.Reset =>
                Debugger.reset(buildScript.debugger.get, dbgConnect)
              case Target.Connect =>
                Debugger.connect(buildScript.debugger.get, dbgConnect)
              case Target.Softdevice =>
                Debugger.programSoftDevice(buildScript.debugger.get, dbgConnect, softDeviceHex)
            }) match {
              case Failure(f) =>
                panic(f.getMessage)
              case _ =>
            }
          }
        }
    }

    info("succeeded")

    if ( cmdlOpts.targets.contains(Target.QteStart) ) Try {
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

    System.exit(0)
  }

  def preprocessUccmBoardFiles(targetBoard:String,uccmHome:File) : List[UccmPragma] = {

    val boardDir = new File(uccmHome,"uccm/board")
    val localBoardDir = new File(getCurrentDirectory,"board")

    def f(s:Stream[File]) : Stream[List[UccmPragma]] =  s match {
      case xs #:: t => Pragmas.extractFrom(xs).toList #:: f(t)
      case _ => Stream.empty
    }

    val localBoardFiles =
      if ( localBoardDir.exists && localBoardDir.isDirectory )
        localBoardDir.listFiles.toList
      else
        Nil

    val boardPragmas = {
      if ( boardDir.exists && boardDir.isDirectory )
        f((localBoardFiles ++ boardDir.listFiles.toList).toStream)
      else
        Stream.empty
    } find {
        _.exists {
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
