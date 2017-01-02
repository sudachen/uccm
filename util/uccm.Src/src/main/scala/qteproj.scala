package com.sudachen.uccm.qteproj

import com.sudachen.uccm.compiler.Compiler
import com.sudachen.uccm.buildscript.{BuildScript,BuildConfig}
import java.io.{File, FileWriter}
import sys.process._
import scala.xml.dtd.{DocType, PublicID}

object QtProj {

  def generate(mainFile:File, buildScript:BuildScript, buildDir:File,
               expand: String => String, verbose: String => Unit ) : Unit = {

    def rightSlash(s:String) = s.map{x => if (x != '\\') x else '/' }

    def local(str:String) = rightSlash(str) match {
      case s => if (s.startsWith("./")) s.drop(2) else s
    }

    val projPrefix = mainFile.getName.take(mainFile.getName.lastIndexOf("."))
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
      val cc = expand(Compiler.ccPath(buildScript.ccTool))
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
      verbose(cmdl)
      cmdl!pl
      wr.close()
    }

    def generateConfig() = {
      val wr = new FileWriter(f_config, false)
      val cc = expand(Compiler.ccPath(buildScript.ccTool))
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

      verbose(cmdl)
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

    val env_uuid = "{83f78507-166f-4c5c-8568-06b199440095}"
    val conf_uuid = "{296b767f-08bf-48e2-a566-3e4dc7192e6b}"
    val workDir = mainFile.getAbsoluteFile.getParentFile.getCanonicalFile
    val uccmCmd = new File(workDir,"uccm.cmd")

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
                    <value type="QString" key="ProjectExplorer.ProcessStep.Arguments"></value>
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
                    <value type="QString" key="ProjectExplorer.ProcessStep.Arguments">--rebuild --qte</value>
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
              </valuemap>

              <value type="int" key="ProjectExplorer.Target.RunConfigurationCount">1</value>

              <valuemap type="QVariantMap" key="ProjectExplorer.Target.DeployConfiguration.0">
                <valuemap type="QVariantMap" key="ProjectExplorer.BuildConfiguration.BuildStepList.0">
                  <valuemap type="QVariantMap" key="ProjectExplorer.BuildStepList.Step.0">
                    <value type="bool" key="ProjectExplorer.BuildStep.Enabled">true</value>
                    <value type="QString" key="ProjectExplorer.ProcessStep.Arguments">--flash --reset</value>
                    <value type="QString" key="ProjectExplorer.ProcessStep.Command">{uccmCmd.getPath}</value>
                    <value type="QString" key="ProjectExplorer.ProcessStep.WorkingDirectory">{workDir.getPath}</value>
                    <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Write firmware into uC Memory</value>
                    <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName">Flash</value>
                    <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.ProcessStep</value>
                  </valuemap>
                  <value type="int" key="ProjectExplorer.BuildStepList.StepsCount">1</value>
                  <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Deploy</value>
                  <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
                  <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">ProjectExplorer.BuildSteps.Deploy</value>
                </valuemap>
                <value type="int" key="ProjectExplorer.BuildConfiguration.BuildStepListCount">1</value>
                <value type="QString" key="ProjectExplorer.ProjectConfiguration.DefaultDisplayName">Upload onto uC</value>
                <value type="QString" key="ProjectExplorer.ProjectConfiguration.DisplayName"></value>
                <value type="QString" key="ProjectExplorer.ProjectConfiguration.Id">DeployToGenericLinux</value>
              </valuemap>

              <value type="int" key="ProjectExplorer.Target.DeployConfigurationCount">1</value>

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