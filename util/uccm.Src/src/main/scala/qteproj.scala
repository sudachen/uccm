package com.sudachen.uccm.qteproj

import com.sudachen.uccm.compiler.Compiler
import com.sudachen.uccm.buildscript.{BuildScript,BuildConfig}
import java.io.{File, FileWriter}
import sys.process._

object QtProj {

  def generate(mainFile:File, buildScript:BuildScript, buildDir:File, expand: String => String ) : Unit = {

    def unquote(s:String) = if ( s.startsWith("\"") ) s.drop(1).dropRight(1) else s

    def generateIncludes() = {
      val rx = "-I\\s*(\\\"[^\\\"]+\\\"|[^\\\"\\s]+)".r
      val wr = new FileWriter(new File(mainFile.getParent, mainFile.getName + ".includes"), true)
      rx.findAllMatchIn(buildScript.cflags.mkString(" ")).foreach{
        x => wr.write((unquote(x.group(1))+"\n").replace("\\","/"))
      }
      wr.write(expand(Compiler.incPath(buildScript.ccTool))+"\n")
      wr.close()
    }

    def generateFiles() = {
      def rightSlash(s:String) = s.map{x => if (x != '\\') x else '/' }
      val wr = new FileWriter(new File(mainFile.getParent, mainFile.getName + ".files"), true)
      buildScript.sources.foreach{ x=> wr.write((x+"\n").replace("\\","/")) }
      val cc = expand(Compiler.ccPath(buildScript.ccTool))
      val cmdl = cc + " -M " + buildScript.cflags.mkString(" ") + " " + mainFile.getPath
      val rx = ("("+rightSlash(buildDir.getPath) + "/\\S+.h)\\s*").r
      val pl = ProcessLogger(s =>
        rx.findFirstMatchIn(rightSlash(s)) match {
          case Some(path) => wr.write(path+"\n")
          case None =>
        },
        s => Unit)
      cmdl!pl
      wr.close()
    }

    def generateConfig() = {
      val rx = "-D\\s*(\\w+)(\\s*=\\s*(\\\"[^\\\"]+\\\"|[^\\\"\\s]+))?".r
      val wr = new FileWriter(new File(mainFile.getParent, mainFile.getName + ".config"), true)
      rx.findAllMatchIn(buildScript.cflags.mkString(" ")).foreach{x=>
        x.group(3) match {
          case null => wr.write(s"#define ${x.group(1)}\n")
          case _ => wr.write(s"#define ${x.group(1)} ${x.group(3)}\n")
        }
      }
      wr.close()
    }

    List(".creator",".config",".includes",".files",".user") foreach { x =>
      val f = new File(mainFile.getName + x)
      if (f.exists) f.delete
    }

    val wr = new FileWriter(new File(mainFile.getParent, mainFile.getName + ".creator"), true)
    wr.write("[General]")
    wr.close()

    generateConfig()
    generateIncludes()
    generateFiles()

    // generating .creator.user file
  }
}