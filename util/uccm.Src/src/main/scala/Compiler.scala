package com.sudachen.uccm

import java.io.File

import scala.sys.process._
import scala.util.{Failure, Success, Try}

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

  private[Compiler] lazy val findArmccHome: Try[Option[String]] = Try {
    var armccPath: Option[String] = None
    val rx = "^\\s*Path\\s+REG_SZ\\s+(.+)$".r
    val where = List(
      "HKLM\\SOFTWARE\\WOW6432Node\\Keil\\Products\\MDK",
      "HKLM\\SOFTWARE\\Keil\\Products\\MDK")

    where.map { x =>
      var path: Option[String] = None
      val cmdl = "reg query " + x + " /v Path"
      cmdl ! ProcessLogger(s => s match {
        case rx(p) => path = Some(p)
        case _ =>
      }, s => Unit)
      path
    }.collectFirst { case Some(x) => x } match {
      case Some(keilHome) =>
        val dir = new File(keilHome + "\\" + "ARMCC")
        if (dir.exists)
          Some(dir.getAbsolutePath)
        else
          None
      case None => None
    }
  }

  private[Compiler] def getArmCli(tool:String): Option[String] = {
    findArmccHome match {
      case Success(home) => home match {
        case Some(s) =>
          val f = new File(s+"\\bin\\"+tool)
          if ( f.exists )
            Some(Util.winExePath(f.getAbsolutePath))
          else
            None
        case None => None
      }
      case Failure(e) =>
        BuildConsole.stackTrace(e.getStackTrace)
        BuildConsole.panic(s"error occured ${e.getMessage}")
        None
    }
  }

  private[Compiler] lazy val armCC: String = getArmCli("armcc.exe") match {
    case Some(cli) => cli
    case None =>
      BuildConsole.panic("armcc software is not installed"); ""
  }

  private[Compiler] lazy val armLink: String = getArmCli("armlink.exe") match {
    case Some(cli) => cli
    case None =>
      BuildConsole.panic("armlink software is not installed"); ""
  }

  private[Compiler] lazy val armAsm: String = getArmCli("armasm.exe") match {
    case Some(cli) => cli
    case None =>
      BuildConsole.panic("armasm software is not installed"); ""
  }

  private[Compiler] lazy val fromElf: String = getArmCli("fromelf.exe") match {
    case Some(cli) => cli
    case None =>
      BuildConsole.panic("fromelf software is not installed"); ""
  }

  private[Compiler] def getGccCli(tool:String): String =
    Components.dflt.getComponentHome("gcc") match {
    case Some(home) =>
      val f = new File(home+"\\bin\\arm-none-eabi-"+tool)
      Util.winExePath(f.getAbsolutePath)
    case None =>
      BuildConsole.panic("gcc component is not installed"); ""
  }

  def incPath(kind:Value):List[String] = kind match {
    case ARMCC => findArmccHome match {
      case Success(p) if p.isDefined => List(p.get + "\\include")
      case _ => Nil
    }
    case GCC => Components.dflt.getComponentHome("gcc") match {
      case None => Nil
      case Some(gccHome) =>
        val f = new File(gccHome+"\\lib\\gcc\\arm-none-eabi")
        List(gccHome+"\\arm-none-eabi\\include") ++
          f.listFiles.filter{ _.isDirectory }.map{ x=> new File(x,"include").getAbsolutePath }
    }
   }

  def ccPath(kind:Value):String = kind match {
    case ARMCC => armCC
    case GCC => getGccCli("gcc.exe")
  }

  def asmPath(kind:Value):Option[String] = kind match {
    case ARMCC => Some(armAsm)
    case GCC => None
  }

  def odmpPath(kind:Value):Option[String] = kind match {
    case GCC =>
      val objdump = getGccCli("objdump.exe")
      Some(objdump)
    case ARMCC => None
  }

  def ldPath(kind:Value):String = kind match {
    case ARMCC => armLink
    case GCC => getGccCli("gcc.exe")
  }

  def elfToHexCmdl(kind:Value,elfFile:File,outFile:File):String = kind match {
    case ARMCC => fromElf + s" --i32 --output ${outFile.getCanonicalPath} ${elfFile.getCanonicalPath}"
    case GCC => getGccCli("objcopy.exe") + s" -O ihex ${elfFile.getCanonicalPath} ${outFile.getCanonicalPath}"
  }

  def elfToBinCmdl(kind:Value,elfFile:File,outFile:File):String = kind match {
    case ARMCC => fromElf + s" --bin --output ${outFile.getCanonicalPath} ${elfFile.getCanonicalPath}"
    case GCC => getGccCli("objcopy.exe") + s" -O binary ${elfFile.getCanonicalPath} ${outFile.getCanonicalPath}"
  }

  def exists(kind:Value) : Boolean = (kind match {
    case GCC => Components.dflt.getComponentHome("gcc")
    case ARMCC => getArmCli("armcc.exe")
  }) match {
    case Some(_) => true
    case None => false
  }

  def install(kind:Value) : Boolean = kind match {
    case GCC =>
      Components.dflt.getComponentHome("gcc") match {
        case Some(home) => true
        case None => Components.dflt.acquireComponent("gcc")
      }
    case ARMCC =>
      BuildConsole.panic("you have to install Keil V5 to use ARMCC"); true
  }
}

