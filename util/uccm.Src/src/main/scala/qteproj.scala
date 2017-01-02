package com.sudachen.uccm.qteproj

import com.sudachen.uccm.compiler.Compiler
import com.sudachen.uccm.buildscript.{BuildScript,BuildConfig}
import java.io.{File, FileWriter}
import sys.process._

object QtProj {

  def generate(mainFile:File, buildScript:BuildScript, buildDir:File,
               expand: String => String, verbose: String => Unit ) : Unit = {

    def rightSlash(s:String) = s.map{x => if (x != '\\') x else '/' }

    def local(str:String) = rightSlash(str) match {
      case s => if (s.startsWith("./")) s.drop(2) else s
    }

    val projPrefix = mainFile.getName.take(mainFile.getName.lastIndexOf("."))
    def unquote(s:String) = if ( s.startsWith("\"") ) s.drop(1).dropRight(1) else s

    def generateIncludes() = {
      val rx = "-I\\s*(\\\"[^\\\"]+\\\"|[^\\\"\\s]+)".r
      val wr = new FileWriter(new File(mainFile.getParent, projPrefix + ".includes"), false)
      rx.findAllMatchIn(buildScript.cflags.mkString(" ")).foreach{
        x => wr.write((unquote(x.group(1))+"\n").replace("\\","/"))
      }
      Compiler.incPath(buildScript.ccTool).foreach( x=> wr.write(expand(x)+"\n") )
      wr.close()
    }

    def generateFiles() = {
      val wr = new FileWriter(new File(mainFile.getParent, projPrefix + ".files"), false)
      buildScript.sources.foreach{ x=> wr.write(local(x)+"\n") }
      val cc = expand(Compiler.ccPath(buildScript.ccTool))
      val cmdl = cc + " -M " + buildScript.cflags.mkString(" ") + " " + mainFile.getPath
      val where = local(buildDir.getPath)
      val rx = ("("+where+"/[/\\w]+.h)").r
      val pl = ProcessLogger(s =>
        rx.findAllMatchIn(rightSlash(s)).foreach {
          case rx(path) => wr.write(path+"\n")
          case _ =>
        },
        s => Unit)
      verbose(cmdl)
      cmdl!pl
      wr.close()
    }

    def generateConfig() = {
      val wr = new FileWriter(new File(mainFile.getParent, projPrefix + ".config"), false)
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

    List(".creator",".config",".includes",".files",".user") foreach { x =>
      val f = new File(mainFile.getName + x)
      if (f.exists) f.delete
    }

    val wr = new FileWriter(new File(mainFile.getParent, projPrefix + ".creator"), false)
    wr.write("[General]")
    wr.close()

    generateConfig()
    generateIncludes()
    generateFiles()

    // generating .creator.user file
  }
}