package com.sudachen.uccm
import java.io.{File,FileWriter}
import org.apache.commons.io.FileUtils
import sys.process._

class UccmPragma
case class UccmHome(tag:String, value:String) extends UccmPragma
case class UccmBoard(tag:String, value:String) extends UccmPragma
case class UccmXcflags(tag:String, value:String) extends UccmPragma
case class UccmCflags(value:String) extends UccmPragma
case class UccmLdflags(value:String) extends UccmPragma
case class UccmRequire(tag:String,value:String) extends UccmPragma
case class UccmAppend(tag:String, value:String) extends UccmPragma
case class UccmDefault(tag:String, value:String) extends UccmPragma
case class UccmInfo(tag:String, value:String) extends UccmPragma
case class UccmDownload(tag:String, value:String) extends UccmPragma

object Pragmas {
  def extractFrom(file:File) : Stream[UccmPragma] = {
    val f = io.Source.fromFile(file)

    def js(s: Stream[String]) : Stream[String] = s match  {
      case xs #:: t if xs.endsWith("\\") => (xs.dropRight(1) + js(t).head) #:: js(t).tail
      case xs #:: t => xs #:: js(t)
      case _ => f.close; Stream.empty
    }

    val rXcflags = "#pragma\\s*uccm\\s*xcflags\\(([\\*\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rBoard   = "#pragma\\s*uccm\\s*board\\(([\\*\\w]+)\\)\\s*=\\s*(.+)$".r
    val rHome    = "#pragma\\s*uccm\\s*home\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rCflags  = "#pragma\\s*uccm\\s*cflags\\s*\\+?=\\s*(.+)$".r
    val rLdflags = "#pragma\\s*uccm\\s*ldflags\\s*\\+?=\\s*(.+)$".r
    val rRequire = "#pragma\\s*uccm\\s*require\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rFile    = "#pragma\\s*uccm\\s*file\\(([\\.\\-\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rDefault = "#pragma\\s*uccm\\s*default\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rInfo    = "#pragma\\s*uccm\\s*info\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rDownload= "#pragma\\s*uccm\\s*download\\((\\w+)\\)\\s*=\\s*(.+)$".r

    def ns(s:String) = {
      val ss = s.dropRight(s.length-s.lastIndexWhere { _ !=  ' ' }-1)
      if ( ss.length >= 2 && ss.startsWith("\"") && ss.endsWith("\"") )
        ss.drop(1).dropRight(1)
      else
        ss
    }

    def parse(s:String): Option[UccmPragma] = s match {
      case rXcflags(tag,value) => Some(UccmXcflags(tag,ns(value)))
      case rBoard(tag,value) => Some(UccmBoard(tag,ns(value)))
      case rHome(tag,value) => Some(UccmHome(tag,ns(value)))
      case rCflags(value) => Some(UccmCflags(value))
      case rLdflags(value) => Some(UccmLdflags(value))
      case rRequire(tag,value) => Some(UccmRequire(tag,ns(value)))
      case rFile(tag,value) => Some(UccmAppend(tag,ns(value)))
      case rDefault(tag,value) => Some(UccmDefault(tag,ns(value)))
      case rInfo(tag,value) => Some(UccmInfo(tag,ns(value)))
      case rDownload(tag,value) => Some(UccmDownload(tag,ns(value)))
      case _ => None
    }

    def qs(s: Stream[String]) : Stream[Option[UccmPragma]] = s match {
      case xs #:: t => parse(xs) #:: qs(t)
      case _ => Stream.empty
    }

    qs(js(f.getLines().toStream)).filter{_.isDefined}.map{_.get}
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

  def stringify(kind:Value):String = kind match {
    case GCC => "gcc"
    case ARMCC => "armcc"
  }

  def ccPath(kind:Value):String = kind match {
    case ARMCC => "[armcc]\\bin\\armcc.exe"
    case GCC => "[gcc]\\bin\\arm-none-eabi-gcc.exe"
  }

  def ldPath(kind:Value):String = kind match {
    case ARMCC => "[armcc]\\bin\\armlink.exe"
    case GCC => "[gcc]\\bin\\arm-none-eabi-gcc.exe"
  }

  def elfToHexCmdl(kind:Value,elfFile:File,outFile:File):String = kind match {
    case ARMCC => s"[armcc]\\bin\\fromelf.exe --vhx --output ${outFile.getCanonicalPath} ${elfFile.getCanonicalPath}"
    case GCC => s"[gcc]\\bin\\arm-none-eabi-objcopy -O ihex ${elfFile.getCanonicalPath} ${outFile.getCanonicalPath}"
  }

  def elfToBinCmdl(kind:Value,elfFile:File,outFile:File):String = kind match {
    case ARMCC => s"[armcc]\\bin\\fromelf.exe --bin --output ${outFile.getCanonicalPath} ${elfFile.getCanonicalPath}"
    case GCC => s"[gcc]\\bin\\arm-none-eabi-objcopy -O binary ${elfFile.getCanonicalPath} ${outFile.getCanonicalPath}"
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

case class BuildScript(ccTool:Compiler.Value,
                       cflags:List[String] = Nil,
                       sources:List[String] = Nil,
                       modules:List[String] = Nil,
                       libraries:List[String] = Nil,
                       generated:List[(String,String)] = Nil,
                       begin:List[String] = Nil,
                       end:List[String] = Nil,
                       ldflags:List[String] = Nil) {
  def toXML : scala.xml.Node = {
    <uccm>
      <cctool>
        {Compiler.stringify(ccTool)}
      </cctool>
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
  def fromXML(xml: scala.xml.Node) : BuildScript = {
    def bs(c:Char):Boolean = c match { case ' '|'\n'|'\r' => true case _ => false }
    def ns(s:String) = s.dropWhile{bs}.reverse.dropWhile{bs}.reverse
    BuildScript(
      Compiler.fromString(ns((xml\"cctool" ).text)).get,
      cflags = (xml\"cflags"\"flag").map{ x => ns(x.text)}.toList,
      ldflags = (xml\"ldflags"\"flag").map{ x => ns(x.text)}.toList,
      sources = (xml\"sources"\"file").map{ x => ns(x.text)}.toList,
      modules = (xml\"modules"\"file").map{ x => ns(x.text)}.toList,
      libraries = (xml\"libraries"\"file").map{ x => ns(x.text)}.toList,
      generated = (xml\"generated"\"append").map{ x => (ns((x\"name").text),ns((x\"content").text))}.toList
    )}
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
                       verbose:  Boolean = false
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
        text("use ARM-NONE-EABI GNU C compiler")

      opt[Unit]("armcc").
        action( (_,c) => c.copy(compiler = Some(Compiler.ARMCC))).
        text("use KeilV5 armcc compiler")

      opt[Unit]("stlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.STLINK))).
        text("use STM ST-Link debugger")

      opt[Unit]("jlink").
        action( (_,c) => c.copy(debugger = Some(Debugger.JLINK))).
        text("use SEGGER J-Link debugger")

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

  def act(cmdlOpts: CmdlOptions) : Unit = {

    val mainFile : File = cmdlOpts.mainFile match {
      case Some(f) => if ( f.isAbsolute ) f else f.getAbsoluteFile
      case None => new File("main.c").getAbsoluteFile
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
    val uccmHome = new File("c:/projects/rnd/uccm")

    val boardPragmas : List[UccmPragma] = preprocessUccmBoardFiles(targetBoard,uccmHome)

    if ( boardPragmas.isEmpty )
      panic(s"unknown board $targetBoard")

    println(s"uccm is working now for board $targetBoard")

    val buildDir = new File(mainFile.getParentFile,
      if ( cmdlOpts.buildConfig == BuildConfig.Release ) s"~Release/$targetBoard"
      else s"~Debug/$targetBoard"
    )

    if ( !buildDir.exists ) buildDir.mkdirs()
    val objDir = new File(buildDir,"obj")
    val incDir = new File(buildDir,"inc")
    val targetElf = new File(buildDir,"firmware.elf")
    val targetHex = new File(buildDir,"firmware.hex")
    val targetBin = new File(buildDir,"firmware.bin")

    if ( cmdlOpts.targets.contains(Target.Rebuild) && buildDir.exists ) {
      FileUtils.deleteDirectory(objDir)
      FileUtils.deleteDirectory(incDir)
      buildDir.listFiles{_.isFile}.foreach{_.delete}
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
      (("@inc" -> Component(home = Some(incDir.getCanonicalPath))),
      ("@obj" -> Component(home = Some(objDir.getCanonicalPath))),
      ("@build" -> Component(home = Some(buildDir.getCanonicalPath))),
      ("@src" -> Component(home = Some(mainFile.getParentFile.getCanonicalPath))))

    def dirExists(s:String):Boolean = { val f = new File(s); f.exists && f.isDirectory }

    def expandHome(s:String):String = "(\\[([@\\w]+)\\])".r findFirstMatchIn s match {
      case None => s
      case Some(m) => components.get(m.group(2)) match {
        case Some(Component(None, info, Some(url))) =>
          panic(s"looks like required to download ${m.group(2)} from $url");s
        case Some(Component(Some(home), _, u)) =>
          expandEnv(home) match {
            case Some(path) if dirExists(path) => expandHome(s.replace(m.group(1), path))
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

    val buildScriptFile = new File(buildDir,"script.xml")
    if ( !buildScriptFile.exists || cmdlOpts.targets.contains(Target.Rebuild) )
      println(s"uccm is using ${Compiler.stringify(targetCompiler)} compiler")

    def extractFromPreprocessor : BuildScript = {
      val cc = expandHome(Compiler.ccPath(targetCompiler))
      val tempFile = File.createTempFile("uccm",".gcc.i")
      val mainFilePath = mainFile.getCanonicalPath
      val gccPreprocCmdline = cc + " -E " + xcflags.map{expandHome}.mkString(" ") + " " + mainFilePath

      println(s"preprocessing main C-file ...")
      verbose(gccPreprocCmdline)
      if ( 0 != (gccPreprocCmdline #> tempFile).! )
        panic("failed to preprocess main C-file")

      Pragmas.extractFrom(tempFile).foldLeft(BuildScript(targetCompiler,List(s"-I${uccmHome.getCanonicalPath}"),List(mainFilePath))) {
        (bs,prag) => prag match {
          case UccmXcflags(x,value) => Compiler.fromString(x) match {
            case Some(`targetCompiler`) => bs.copy(cflags = value :: bs.cflags)
            case None if x == "*" => bs.copy(cflags = value :: bs.cflags)
            case _ => bs
          }
          case UccmCflags(value) => bs.copy(cflags = value :: bs.cflags)
          case UccmLdflags(value) => bs.copy(ldflags = value :: bs.ldflags)
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
          cflags = bs.cflags.map{expandHome}.reverse,
          ldflags = bs.ldflags.map{expandHome}.reverse,
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
    else
      println(s"uccm is using ${buildScript.ccTool} compiler")


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
      val srcFile = new File(source)
      val objFileName = srcFile.getName + ".obj"
      val objFile = new File(objDir,objFileName)
      if ( cmdlOpts.targets.contains(Target.Rebuild) ||
        !objFile.exists || FileUtils.isFileOlder(objFile,srcFile) ) {
        println(s"compiling ${srcFile.getName} ...")
        val gccCmdline: String = (List(cc, "-c ") ++
          buildScript.cflags.map{expandHome} ++
          List(srcFile.getCanonicalPath, "-o", objFile.getCanonicalPath)).mkString(" ")
        verbose(gccCmdline)
        if (0 != gccCmdline.!)
          panic(s"failed to compile ${srcFile.getName}")
      }
      objFile.getName :: ls
    }

    val objects = buildScript.sources.foldLeft(List[String]()) { compile }
    val modules = buildScript.modules.foldLeft(List[String]()) { compile }
    val ld = expandHome(Compiler.ldPath(buildScript.ccTool))
    val objFiles = ( objects ++ modules ).map{fn => new File(objDir,fn)}
    val haveToRelink = cmdlOpts.targets.contains(Target.Rebuild) ||
      !targetElf.exists ||
      objFiles.foldLeft( false ) {(f,obj) => f || FileUtils.isFileOlder(targetElf,obj)}

    if ( haveToRelink ) {
      println("linking ...")
      List(targetBin,targetHex,targetElf).foreach{f => if (f.exists) f.delete}
      val gccCmdline = (List(ld) ++
        buildScript.ldflags.map{expandHome} ++ objFiles ++
        List("-o", targetElf.getCanonicalPath)).mkString(" ")
      verbose(gccCmdline)
      if (0 != gccCmdline.!)
        panic(s"failed to link ${targetElf.getName}")
      val toHexCmdl = expandHome(Compiler.elfToHexCmdl(buildScript.ccTool,targetElf,targetHex))
      verbose(toHexCmdl)
      if (0 != toHexCmdl.!)
        panic(s"failed to generate ${targetHex.getName}")
      val toBinCmdl = expandHome(Compiler.elfToBinCmdl(buildScript.ccTool,targetElf,targetBin))
      verbose(toBinCmdl)
      if (0 != toBinCmdl.!)
        panic(s"failed to generate ${targetBin.getName}")
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