package com.sudachen.uccm.qteproj

import com.sudachen.uccm.buildscript.BuildScript
import java.io.{File, FileWriter}

object QtProj {

  def generate(mainFile:File,buildScript:BuildScript) : Unit = {

    def unquote(s:String) =
      if ( s.startsWith("\"") )
        s.drop(1).dropRight(1)
      else
        s

    def generateIncludes() = {
      val rx = "-I\\s*(\\\"[^\\\"]+\\\"|[^\\\"\\s]+)".r
      val wr = new FileWriter(new File(mainFile.getParent, mainFile.getName + ".includes"), true)
      rx.findAllMatchIn(buildScript.cflags.mkString(" ")).foreach{
        x => wr.write((unquote(x.group(1))+"\n").replace("\\","/"))
      }
      wr.close()
    }

    def generateFiles() = {
      val wr = new FileWriter(new File(mainFile.getParent, mainFile.getName + ".files"), true)
      buildScript.sources.foreach{ x=> wr.write((x+"\n").replace("\\","/")) }
      wr.close()
    }

    def generateUser() = {

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
    generateUser()
  }
}