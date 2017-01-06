package com.sudachen.uccm.debugger
import java.io.File

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import sys.process._

object Debugger extends Enumeration {
  val STLINK, JLINK, NRFJPROG = Value

  private[Debugger] def ns(s:String):String = s.dropWhile{_.isSpaceChar}.reverse.dropWhile{_.isSpaceChar}.reverse
  private[Debugger] def quote(s:String):String = "\"" + s + "\""
  private[Debugger] def winExePath(s:String):String = quote( s.map{ case '/' => '\\' case '\"' => '\0' case c => c }.filter{ _ != '\0' } )

  def fromString(name:String): Option[Value] = name match {
    case "stlink" => Some(STLINK)
    case "jlink" => Some(JLINK)
    case "nrfjprog" => Some(NRFJPROG)
    case _ => None
  }

  def stringify(kind:Value):String = kind match {
    case JLINK => "jlink"
    case STLINK => "stlink"
    case NRFJPROG => "nrfjprog"
  }

  private[Debugger] def envGetOnOf(names:List[String]):List[String] =
    names.map{x => sys.env.get(x)}.filter{_.isDefined}.map{_.get}


  private[Debugger] lazy val nrfJprog = winExePath(findNrfjprog())
  private[Debugger] def findNrfjprog() : String = {
    var jprogPath:Option[String] = None
    val rx = "InstallPath\\s+REG_SZ\\s+(\\.+)$".r

    val pl = ProcessLogger(s => s match {
        case rx(installPath) =>
          jprogPath = Some(installPath)
        case _ =>
      },
      s => Unit)

    val cmdl = """reg query "HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Nordic Semiconductor\nrfjprog" /s /v InstallPath"""
    println(cmdl)
    if ( 0 != (cmdl!pl))
      throw new RuntimeException("no nrfjprog found")

    jprogPath match {
      case Some(s) => ns(s)
      case None =>
        throw new RuntimeException("no nrfjprog found")
    }
  }

  private[Debugger] lazy val stLinkCli = winExePath(findStLinkCli())
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

  def program(kind:Value,firmwareHex:File,verbose:String=>Unit,doReset:Boolean,connopt:List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      var hardRst = if (doReset) "-HardRst" else ""
      val cmdl = (List(stLinkCli)++connopt++List("-c SWD UR","-P",firmwareHex.getPath,hardRst)).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      var rst = if (doReset) "-reset" else ""
      val cmdl = (List(nrfJprog)++connopt++List("--program",firmwareHex.getPath,"--sectorerase",rst)).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute jnrfprog command")
  })

  def reset(kind:Value,verbose:String=>Unit,connopt:List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      val cmdl = (List(stLinkCli)++connopt++List("-c SWD UR","-HardRst")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      val cmdl = (List(nrfJprog)++connopt++List("--reset")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute jnrfprog command")
  })

  def connect(kind:Value,verbose:String=>Unit,connopt:List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      val cmdl = (List(stLinkCli)++connopt++List("-c SWD HOTPLUG","-SCore -CoreReg")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      val cmdl = (List(nrfJprog)++connopt++List("--readregs")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute jnrfprog command")
  })

  def erase(kind:Value,verbose:String=>Unit,connopt:List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      val cmdl = (List(stLinkCli)++connopt++List("-c SWD UR","-ME")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      val cmdl = (List(nrfJprog)++connopt++List("-e")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute jnrfprog command")
  })

  def reinit(kind:Value,verbose:String=>Unit,connopt:List[String],vendorwareHex:Option[File]) : Try[Unit] = Try(kind match {
    case NRFJPROG =>
      val cmdl =
        if ( vendorwareHex.isDefined )
          (List(nrfJprog)++connopt++List("--program",vendorwareHex.get.getPath,"--chiperase")).mkString(" ")
        else
          (List(nrfJprog)++connopt++List("--chiperase")).mkString(" ")
      verbose(cmdl)
      if ( 0 != cmdl.! )
        throw new RuntimeException("failed to execute jnrfprog command")
  })
}
