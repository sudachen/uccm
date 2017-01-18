package com.sudachen.uccm
import java.io.File
import scala.util.{Failure, Success, Try}
import sys.process._

object Debugger extends Enumeration {
  val STLINK, JLINK, NRFJPROG = Value

  def fromString(name: String): Option[Value] = name match {
    case "stlink" => Some(STLINK)
    case "jlink" => Some(JLINK)
    case "nrfjprog" => Some(NRFJPROG)
    case _ => None
  }

  def stringify(kind: Value): String = kind match {
    case JLINK => "jlink"
    case STLINK => "stlink"
    case NRFJPROG => "nrfjprog"
  }

  private[Debugger] def envGetOnOf(names: List[String]): List[String] =
    names.map { x => sys.env.get(x) }.filter {
      _.isDefined
    }.map {
      _.get
    }

  private[Debugger] def findNrfjprog: Try[Option[String]] = Try {
    val rx = "^\\s*InstallPath\\s+REG_SZ\\s+(.+)$".r
    val where = List(
      "HKLM\\SOFTWARE\\WOW6432Node\\Nordic Semiconductor\\nrfjprog",
      "HKLM\\SOFTWARE\\Nordic Semiconductor\\nrfjprog")

    where.map { x =>
      var path: Option[String] = None
      val cmdl = "reg query \"" + x + "\" /s /v InstallPath"
      cmdl ! ProcessLogger(s => s match {
        case rx(p) => path = Some(p)
        case _ =>
      }, s => Unit)
      path
    }.collectFirst { case Some(x) => x } match {
      case Some(path) =>
        val exeFile = Util.ns(path) + "nrfjprog.exe"
        if (new File(exeFile).exists)
          Some(exeFile)
        else
          None
      case None => None
    }
  }

  private[Debugger] def getNrfjprogCli: Option[String] = {
    findNrfjprog match {
      case Success(optCli) => optCli match {
        case Some(s) => Some(Util.winExePath(s))
        case None => None
      }
      case Failure(e) =>
        BuildConsole.stackTrace(e.getStackTrace)
        BuildConsole.panic(s"error occured ${e.getMessage}")
        None
    }
  }

  private[Debugger] lazy val nrfJprog = getNrfjprogCli match {
    case Some(cli) => cli
    case None =>
      BuildConsole.panic("nrfjprog software is not installed"); ""
  }

  private[Debugger] def findStlinkCli: Try[Option[String]] = Try {

    (sys.env.get("ST_LINK_HOME") match {
      case Some(s) =>
        val exeFile = new File(s + "\\ST-LINK Utility\\ST-LINK_CLI.exe")
        if (exeFile.exists)  Some(exeFile.getAbsolutePath) else None
      case _ => None
    }) .orElse {
      List(
        List("ProgramFiles(x86)", "PROGRAMFILES(x86)"),
        List("ProgramFiles", "PROGRAMFILES")).
        map { env =>
          envGetOnOf(env) match {
            case s :: _ =>
              val exeFile = new File(s + "\\STMicroelectronics\\STM32 ST-LINK Utility\\ST-LINK Utility\\ST-LINK_CLI.exe")
              if (exeFile.exists) Some(exeFile.getAbsolutePath) else None
            case _ => None
          }
        }.collectFirst { case Some(x) => x }
    }
  }

  private[Debugger] def getStlinkCli: Option[String] = {
    findStlinkCli match {
      case Success(optCli) => optCli match {
        case Some(s) => Some(Util.winExePath(s))
        case None => None
      }
      case Failure(e) =>
        BuildConsole.stackTrace(e.getStackTrace)
        BuildConsole.panic(s"error occured ${e.getMessage}")
        None
    }
  }

  private[Debugger] lazy val stLinkCli = getStlinkCli match {
    case Some(cli) => cli
    case None =>
      BuildConsole.panic("stlink software is not installed"); ""
  }

  def program(kind: Value, firmwareHex: File, doReset: Boolean, connopt: List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      var hardRst = if (doReset) "-HardRst" else ""
      val cmdl = (List(stLinkCli) ++ connopt ++ List("-c SWD UR", "-P", firmwareHex.getPath, hardRst)).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      var rst = if (doReset) "--reset" else ""
      val cmdl = (List(nrfJprog) ++ connopt ++ List("--program", firmwareHex.getPath, "--sectorerase", rst)).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute nrfjprog command")
  })

  def reset(kind: Value, connopt: List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      val cmdl = (List(stLinkCli) ++ connopt ++ List("-c SWD UR", "-HardRst")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      val cmdl = (List(nrfJprog) ++ connopt ++ List("--reset")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute nrfjprog command")
  })

  def connect(kind: Value, connopt: List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      val cmdl = (List(stLinkCli) ++ connopt ++ List("-c SWD HOTPLUG", "-SCore -CoreReg")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      val cmdl = (List(nrfJprog) ++ connopt ++ List("--readregs")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute nrfjprog command")
  })

  def erase(kind: Value, connopt: List[String]): Try[Unit] = Try(kind match {
    case STLINK =>
      val cmdl = (List(stLinkCli) ++ connopt ++ List("-c SWD UR", "-ME")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute stlink command")
    case NRFJPROG =>
      val cmdl = (List(nrfJprog) ++ connopt ++ List("-e")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute nrfjprog command")
  })

  def programSoftDevice(kind: Value, connopt: List[String], softDeviceHex: Option[File]): Try[Unit] = Try(kind match {
    case NRFJPROG =>
      val cmdl =
        if (softDeviceHex.isDefined)
          (List(nrfJprog) ++ connopt ++ List("--program", softDeviceHex.get.getPath, "--chiperase")).mkString(" ")
        else
          (List(nrfJprog) ++ connopt ++ List("-e")).mkString(" ")
      BuildConsole.verbose(cmdl)
      if (0 != cmdl.!)
        throw new RuntimeException("failed to execute nrfjprog command")
    case _ =>
      erase(kind, connopt)
  })

  def isRequiredToInstallSoftware(kind: Value): Boolean = kind match {
    case NRFJPROG => getNrfjprogCli.isEmpty
    //case JLINK => getJlinkCli.isEmpty
    case STLINK => getStlinkCli.isEmpty
  }

  def install(kind: Value): Boolean = kind match {
    case NRFJPROG => Components.dflt.acquireComponent("nrfjprog")
    case JLINK => Components.dflt.acquireComponent("jlink")
    case STLINK => Components.dflt.acquireComponent("stlink")
  }
}
