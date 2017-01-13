package com.sudachen.uccm.qteproj

import com.sudachen.uccm.compiler.Compiler
import com.sudachen.uccm.buildscript.{BuildConfig, BuildScript}
import java.io.{File, FileWriter}

import com.sudachen.uccm.buildconsole.BuildConsole
import com.sudachen.uccm.components.Components
import com.sudachen.uccm.debugger.Debugger
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}
import sys.process._
import scala.xml.dtd.{DocType, PublicID}

object QtProj {

  def bs(c:Char):Boolean = c match { case ' '|'\n'|'\r' => true case _ => false }
  def ns(s:String):String = s.dropWhile{bs}.reverse.dropWhile{bs}.reverse
  def quote(s: String): String = "\"" + s + "\""
  def winExePath(s: String): String = quote {
    s.map {
      case '/' => '\\'
      case '\"' => '\u0000'
      case c => c
    }.filter {
      '\u0000'.!=
    }
  }

  def findQteInSystem: Try[Option[String]] = Try {
    val rx = "^\\s*\\(Default\\)\\s+REG_SZ\\s+\"([^\"]+)\".*$".r
    val where = List(
      "HKLM\\SOFTWARE\\Classes\\creator_auto_file\\shell\\open\\command",
      "HKCR\\creator_auto_file\\shell\\open\\command")

    where.map { x =>
      var path: Option[String] = None
      val cmdl = "reg query " + x + " /ve"
      cmdl ! ProcessLogger(s => s match {
        case rx(p) => path = Some(p)
        case _ =>
      }, s => Unit)
      path
    }.collectFirst { case Some(x) => x } match {
      case Some(path) =>
        val exeFile = path
        if (new File(exeFile).exists)
          Some(exeFile)
        else
          None
      case None => None
    }
  }

  lazy val qteInstallDir = new File(BuildScript.uccmRepoDirectoryFile,"uccm-qte420")
  lazy val qteInstalLogFile = new File(BuildScript.uccmRepoDirectoryFile,".uccm-qte420-log.xml")
  lazy val qteSettingsFile = new File(BuildScript.uccmRepoDirectoryFile,".qte")

  case class InstallStatus(editorIsOk:Boolean = false,settingsIsOk:Boolean = false,isReady:Boolean = false) {
    def toXML: xml.Node = {
      <install>
        <editor>{if(editorIsOk) 1 else 0}</editor>
        <settings>{if(settingsIsOk) 1 else 0}</settings>
        <ready>{if(isReady) 1 else 0}</ready>
      </install>
    }
  }

  object InstallStatus {
    def fromXML(x: xml.Node): InstallStatus  = {
      InstallStatus(
        ns((x \ "editor").text).toInt != 0,
        ns((x \ "settings").text).toInt != 0,
        ns((x \ "ready").text).toInt != 0
      )
    }
  }

  def findQte: Try[Option[String]] = Try {
    val qteProgFile = new File(qteInstallDir,"bin\\qtcreator.exe")
    if ( !qteInstallDir.exists || !qteProgFile.exists || !qteInstalLogFile.exists )
      None
    else
    {
      val st = InstallStatus.fromXML(xml.XML.loadFile(qteInstalLogFile))
      if ( st.isReady )
        Some(qteProgFile.getAbsolutePath)
      else
        None
    }
  }

  def getQteProg: Option[String] = {
    findQte match {
      case Success(opt) => opt match {
        case Some(s) => Some(winExePath(s))
        case None => None
      }
      case Failure(e) =>
        BuildConsole.stackTrace(e.getStackTrace)
        BuildConsole.panic(s"error occured ${e.getMessage}")
        None
    }
  }

  lazy val qteExe : String = getQteProg match {
    case Some(prog) => prog
    case None =>
      BuildConsole.panic("QtCreator software is not installed"); ""
  }

  def isRequiredToInstall : Boolean = {
    getQteProg.isEmpty || (Try {
      val st = InstallStatus.fromXML(xml.XML.loadFile(qteInstalLogFile))
      !st.isReady
    } match {
      case Success(b) => b
      case Failure(_) => false
    })
  }

  def install(): Boolean = Try {

    val st =  if ( qteInstalLogFile.exists )
                InstallStatus.fromXML(xml.XML.loadFile(qteInstalLogFile))
              else
                InstallStatus()

    def write(nst:InstallStatus) = xml.XML.save(qteInstalLogFile.getAbsolutePath,nst.toXML)

    (if ( !st.editorIsOk ) {
      if ( qteInstallDir.exists ) FileUtils.deleteDirectory(qteInstallDir)
      if (Components.dflt.acquireComponent("qte420")) {
        val nst = st.copy(editorIsOk = true); write(nst); Some(nst)
      }
      else None
    } else Some(st)) match {
      case None => false
      case Some(nst) =>
        val rx = ".*\\[\\/](\\w+).xml$".r
        val patRepo = "%UCCM100REPO%"
        val repoPath = BuildScript.uccmRepoDirectory.map{ case '\\' => '/' case x => x }
        val patUccm = "%UCCM100HOME%"
        val uccmPath = BuildScript.uccmDirectory.map{ case '\\' => '/' case x => x }
        val arcFile = new File(BuildScript.uccmDirectoryFile,"util/qte420-settings.zip")

        def expand(s:String):String = s.indexOf(patRepo) match {
          case n if n >= 0 =>
            val tail = s.substring(n+patRepo.length)
            s.substring(0,n) + repoPath + expand(tail)
          case _ => s.indexOf(uccmPath) match {
            case n if n >= 0 =>
              val tail = s.substring(n+patRepo.length)
              s.substring(0,n) + uccmPath + expand(tail)
            case _ => s
          }
        }

        def p(n:String) : Option[String => String] = n match {
          case rx(s) =>
            Some( t => expand(t) )
          case _ => None
        }

        Components.unpackZip(arcFile,qteSettingsFile,p) match {
          case Success(_) =>
            write(nst.copy(settingsIsOk = true,isReady = true))
            true
          case Failure(e) =>
            BuildConsole.stackTrace(e.getStackTrace)
            BuildConsole.error(e.getMessage)
            false
        }
    }
  } match {
    case Success(b) => b
    case Failure(e) =>
      BuildConsole.stackTrace(e.getStackTrace)
      BuildConsole.error(e.getMessage)
      false
  }

  def generate(mainFile:File, buildScript:BuildScript, buildDir:File,
               expand: String => String) : Unit = {

    def rightSlash(s:String) = s.map{ case '\\' => '/' case x => x }

    def local(str:String) = rightSlash(str) match {
      case s => if (s.startsWith("./")) s.drop(2) else s
    }

    val projPrefix = buildScript.boardName
    def unquote(s:String) = if ( s.startsWith("\"") ) s.drop(1).dropRight(1) else s
    val f_creator = new File(mainFile.getParent, projPrefix + ".creator")
    val f_creator_user = new File(mainFile.getParent, projPrefix + ".creator.user")
    val f_includes = new File(mainFile.getParent, projPrefix + ".includes")
    val f_files = new File(mainFile.getParent, projPrefix + ".files")
    val f_config = new File(mainFile.getParent, projPrefix + ".config")

    List(f_creator,f_creator_user,f_includes,f_config,f_files).foreach{ x =>
      if ( x.exists ) x.delete
    }

    def generateIncludes() = {
      val rx = "-I\\s*(\\\"[^\\\"]+\\\"|[^\\\"\\s]+)".r
      val wr = new FileWriter(f_includes, false)
      rx.findAllMatchIn(buildScript.cflags.mkString(" ")).foreach{
        x => wr.write((unquote(x.group(1))+"\n").replace("\\","/"))
      }
      Compiler.incPath(buildScript.ccTool).foreach( x=> wr.write(expand(x)+"\n") )
      wr.close()
    }

    def generateFiles() = {
      val wr = new FileWriter(f_files, false)
      buildScript.sources.foreach{ x=> wr.write(local(x)+"\n") }
      buildScript.modules.foreach{ x=> wr.write(local(x)+"\n") }
      val cc = Compiler.ccPath(buildScript.ccTool)
      val cmdl = cc + " -M " + buildScript.cflags.mkString(" ") + " " + mainFile.getPath
      val where = local(buildDir.getPath)
      val rx = ("("+where+"/[/\\w\\.]+.h)").r
      val pl = ProcessLogger(s => {
        rx.findAllMatchIn(rightSlash(s)).foreach {
          case rx(path) =>
            wr.write(path+"\n")
          case _ =>
        }},
        s => Unit)
      BuildConsole.verbose(cmdl)
      cmdl!pl
      wr.close()
    }

    def generateConfig() = {
      val wr = new FileWriter(f_config, false)
      val cc = Compiler.ccPath(buildScript.ccTool)
      val tempFile = File.createTempFile("uccm",s"-${Compiler.stringify(buildScript.ccTool)}.c")
      tempFile.createNewFile()

      val cmdl =
        if ( buildScript.ccTool == Compiler.ARMCC )
          cc + " -E --list-macros " + buildScript.cflags.mkString(" ") + " " + tempFile.getPath
        else
          cc + " -E -dM " + buildScript.cflags.mkString(" ") + " " + tempFile.getPath

      val rx = "^(#define .+)$".r
      val pl = ProcessLogger(s =>
        rx.findFirstMatchIn(s) match {
          case Some(df) => wr.write(df+"\n")
          case None =>
        },
        s => Unit)

      BuildConsole.verbose(cmdl)
      cmdl!pl
      tempFile.delete
      wr.close()
    }

    val wr = new FileWriter(f_creator, false)
    wr.write("[General]")
    wr.close()

    generateConfig()
    generateIncludes()
    generateFiles()

    val env_uuid = "{d8039803-4279-4b90-ad93-64aaffce02ab}"
    val conf_uuid = "{ad2775bb-c909-468e-91cc-e79a31036fe3}"
    val workDir = mainFile.getAbsoluteFile.getParentFile.getCanonicalFile
    val uccmCmd = new File(workDir,"uccm.cmd")

    val softDeviceOpt = if ( buildScript.softDevice == "RAW" ) "--raw" else "--softdevice "+buildScript.softDevice
    val debuggerOpt = if ( buildScript.debugger.isEmpty ) "" else "--"+Debugger.stringify(buildScript.debugger.get)
    val buildCfgOpt = "--"+BuildConfig.stringify(buildScript.config)
    val compilerOpt = "--"+Compiler.stringify(buildScript.ccTool)
    val uccmArgs = List(compilerOpt,softDeviceOpt,debuggerOpt,buildCfgOpt,"-c").mkString(" ")

    val qtUser =
<qtcreator>
  <data>
    <variable>EnvironmentId</variable>
    <value type="QByteArray">{env_uuid}</value>
  </data>
  <data>
    <variable>ProjectExplorer.Project.ActiveTarget</variable>
    <value type="int">0</value>
  </data>
  <data>
    <variable>ProjectExplorer.Project.Target.0</variable>
    <valuemap type="QVariantMap">
      <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">uccm</value>
      <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName">uccm</value>
      <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">{conf_uuid}</value>
      <value type="int" key="ProjectExplorer.Target.ActiveBuildConfiguration">0</value>
      <value type="int" key="ProjectExplorer.Target.ActiveDeployConfiguration">0</value>
      <value type="int" key="ProjectExplorer.Target.ActiveRunConfiguration">0</value>
      <valuemap type="QVariantMap" key="ProjectExplorer.Target.BuildConfiguration.0">
        <value type="QString" key="ProjectExplorer.BuildConfiguration.BuildDirectory">{workDir.getPath}</value>

        <valuemap type="QVariantMap" key="ProjectExplorer.BuildConfiguration.BuildStepList.0">
          <valuemap type="QVariantMap" key="ProjectExplorer.BuildStepList.Step.0">
            <value type="bool" key="ProjectExplorer.BuildStep.Enabled">true</value>
            <value type="QString" key="ProjectExplorer.ProcessStep.Arguments">{uccmArgs}</value>
            <value type="QString" key="ProjectExplorer.ProcessStep.Command">{uccmCmd.getPath}</value>
            <value type="QString" key="ProjectExplorer.ProcessStep.WorkingDirectory">{workDir.getPath}</value>
            <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Custom Process Step</value>
            <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
            <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.ProcessStep</value>
          </valuemap>
          <value type="int" key="ProjectExplorer.BuildStepList.StepsCount">1</value>
          <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Build</value>
          <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
          <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.BuildSteps.Build</value>
        </valuemap>

        <valuemap type="QVariantMap" key="ProjectExplorer.BuildConfiguration.BuildStepList.1">
          <valuemap type="QVariantMap" key="ProjectExplorer.BuildStepList.Step.0">
            <value type="bool" key="ProjectExplorer.BuildStep.Enabled">true</value>
            <value type="QString" key="ProjectExplorer.ProcessStep.Arguments">{uccmArgs} --rebuild --qte</value>
            <value type="QString" key="ProjectExplorer.ProcessStep.Command">{uccmCmd.getPath}</value>
            <value type="QString" key="ProjectExplorer.ProcessStep.WorkingDirectory">{workDir.getPath}</value>
            <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Custom Process Step</value>
            <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
            <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.ProcessStep</value>
          </valuemap>
          <value type="int" key="ProjectExplorer.BuildStepList.StepsCount">1</value>
          <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Clean</value>
          <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
          <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.BuildSteps.Clean</value>
        </valuemap>

        <value type="int" key="ProjectExplorer.BuildConfiguration.BuildStepListCount">2</value>
        <value type="bool" key="ProjectExplorer.BuildConfiguration.ClearSystemEnvironment">false</value>
        <valuelist type="QVariantList" key="ProjectExplorer.BuildConfiguration.UserEnvironmentChanges"/>
        <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">UCCM</value>
        <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName">UCCM</value>
        <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">GenericProjectManager.GenericBuildConfiguration</value>
      </valuemap>
      <value type="int" key="ProjectExplorer.Target.BuildConfigurationCount">1</value>
      <value type="int" key="ProjectExplorer.Target.DeployConfigurationCount">0</value>
      <valuemap type="QVariantMap" key="ProjectExplorer.Target.PluginSettings"/>
      <valuemap type="QVariantMap" key="ProjectExplorer.Target.RunConfiguration.0">
        <value type="int" key="PE.EnvironmentAspect.Base">2</value>
        <valuelist type="QVariantList" key="PE.EnvironmentAspect.Changes"/>
        <value type="QString" key="ProjectExplorer.CustomExecutableRunConfiguration.Arguments">{uccmArgs} --program --reset</value>
        <value type="QString" key="ProjectExplorer.CustomExecutableRunConfiguration.Executable">{uccmCmd.getPath}</value>
        <value type="QString" key="ProjectExplorer.CustomExecutableRunConfiguration.WorkingDirectory">{workDir.getPath}</value>
        <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Program and Reset</value>
        <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
        <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.CustomExecutableRunConfiguration</value>
      </valuemap>
      <value type="int" key="ProjectExplorer.Target.RunConfigurationCount">1</value>
    </valuemap>
  </data>
  <data>
    <variable>ProjectExplorer.Project.TargetCount</variable>
    <value type="int">1</value>
  </data>
  <data>
    <variable>ProjectExplorer.Project.Updater.FileVersion</variable>
    <value type="int">18</value>
  </data>
  <data>
    <variable>Version</variable>
    <value type="int">18</value>
  </data>
</qtcreator>

    scala.xml.XML.save(
      f_creator_user.getPath,
      qtUser,"UTF-8",true,DocType("QtCreatorProject"))
  }
}