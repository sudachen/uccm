package com.sudachen.uccm.debugger
import java.io.File
import scala.util.{Try,Success,Failure}
import sys.process._

object Debugger extends Enumeration {
  val STLINK, JLINK = Value

  private[Debugger] def quote(s:String):String = "\"" + s + "\""
  private[Debugger] def winExePath(s:String):String = quote( s.map{ case '/' => '\\' case '\"' => '\0' case c => c }.filter{ _ != '\0' } )

  def fromString(name:String): Option[Value] = name match {
    case "stlink" => Some(STLINK)
    case "jlink" => Some(JLINK)
    case _ => None
  }

  def stringify(kind:Value):String = kind match {
    case JLINK => "jlink"
    case STLINK => "stlink"
  }

  private[Debugger] def envGetOnOf(names:List[String]):List[String] =
    names.map{x => sys.env.get(x)}.filter{_.isDefined}.map{_.get}

  private[Debugger] def findStLinkCli() : String = {

    sys.env.get("ST_LINK_HOME") match {
      case Some(s) =>
        val exeFile = new File(s+"\\ST-LINK Utility\\ST-LINK_CLI.exe")
        println(exeFile)
        if ( exeFile.exists )
          return exeFile.getAbsolutePath
      case _ =>
    }

    List(
      List("ProgramFiles(x86)","PROGRAMFILES(x86)"),
      List("ProgramFiles","PROGRAMFILES")).
      foreach { x =>
      envGetOnOf(x) match {
        case s :: _ =>
          val exeFile = new File(s + "\\STMicroelectronics\\STM32 ST-LINK Utility\\ST-LINK Utility\\ST-LINK_CLI.exe")
          println(exeFile)
          if (exeFile.exists)
            return exeFile.getAbsolutePath
        case _ =>
      }
    }

    throw new RuntimeException("no ST-link found")
  }

  def upload(kind:Value,firmwareHex:File,verbose:String=>Unit,doReset:Boolean): Try[Unit] = Try(kind match {
    case STLINK =>
      val stlink = findStLinkCli()
      var hardRst = if (doReset) "-HardRst" else ""
      val cmdl = List(winExePath(stlink),"-c SWD UR","-P",firmwareHex.getPath,hardRst).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
  })

  def reset(kind:Value,verbose:String=>Unit): Try[Unit] = Try(kind match {
    case STLINK =>
      val stlink = findStLinkCli()
      val cmdl = List(winExePath(stlink),"-c SWD UR","-HardRst").mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
  })

  def connect(kind:Value,verbose:String=>Unit): Try[Unit] = Try(kind match {
    case STLINK =>
      val stlink = findStLinkCli()
      val cmdl = List(winExePath(stlink),"-c SWD HOTPLUG","-SCore -CoreReg").mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
  })

  def erase(kind:Value,verbose:String=>Unit): Try[Unit] = Try(kind match {
    case STLINK =>
      val stlink = findStLinkCli()
      val cmdl = List(winExePath(stlink),"-c SWD UR","-ME").mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
  })
}
