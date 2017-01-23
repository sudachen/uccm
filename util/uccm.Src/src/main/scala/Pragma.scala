
package com.sudachen.uccm

import java.io.File

class Pragma
object Pragma {

  case class Import(accname:String, modname:String) extends Pragma
  case class Alias(tag:String, value:String) extends Pragma
  case class Home(tag:String, value:String) extends Pragma
  case class Board(tag:String, value:String) extends Pragma
  case class Xcflags(tag:String, value:String) extends Pragma
  case class Cflags(value:String) extends Pragma
  case class Ldflags(value:String) extends Pragma
  case class Asflags(value:String) extends Pragma
  case class Require(tag:String,value:String) extends Pragma
  case class Append(tag:String, value:String) extends Pragma
  case class AppendEx(tag:String, value:String) extends Pragma
  case class Default(tag:String, value:String) extends Pragma
  case class Info(tag:String, value:String) extends Pragma
  case class Download(tag:String, value:String) extends Pragma
  case class SoftDevice(tag:String, value:String) extends Pragma
  case class SoftDeviceAls(tag:String, value:String) extends Pragma
  case class Debugger(tag:String, value:String) extends Pragma
  case class Let(tag:String, value:String) extends Pragma
  case class LetIfNo(tag:String, value:String) extends Pragma

  def extractFromTempFile(tempFile:File) : Stream[Pragma] = {
    def js(s: Stream[Pragma]) : Stream[Pragma] = s match {
      case xs #:: t => xs #:: js(t)
      case _ => tempFile.delete; Stream.empty
    }
    js(extractFrom(tempFile))
  }

  def extractStrings(file:File) : Stream[String] = {
    val f = io.Source.fromFile(file)

    def js(s: Stream[String]) : Stream[String] = s match  {
      case xs #:: t if xs.endsWith("\\") => (xs.dropRight(1) + js(t).head) #:: js(t).tail
      case xs #:: t => xs #:: js(t)
      case _ => f.close; Stream.empty
    }

    js(f.getLines.toStream)
  }

  def extractFrom(file:File) : Stream[Pragma] = {
    def js: Stream[String] = extractStrings(file)

    val rXcflags = "#pragma\\s+uccm\\s+xcflags\\(([\\*\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rDebugger= "#pragma\\s+uccm\\s+debugger\\(([\\*\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rBoard   = "#pragma\\s+uccm\\s+board\\(([\\*\\w]+)\\)\\s*=\\s*(.+)$".r
    val rSoftDevice  = "#pragma\\s+uccm\\s+softdevice\\(([\\+\\w]+)\\)\\s*=\\s*(.+)$".r
    val rSoftDeviceAls  = "#pragma\\s+uccm\\s+softdevice\\(([\\+\\w]+)\\)\\s*=>\\s*(.+)$".r
    val rHome    = "#pragma\\s+uccm\\s+home\\(([\\+\\.\\w]+)\\)\\s*=\\s*(.+)$".r
    val rAlias   = "#pragma\\s+uccm\\s+alias\\(([A-Z_0-9]+)\\)\\s*=\\s*(.+)$".r
    val rCflags  = "#pragma\\s+uccm\\s+cflags\\s*\\+?=\\s*(.+)$".r
    val rLdflags = "#pragma\\s+uccm\\s+ldflags\\s*\\+?=\\s*(.+)$".r
    val rAsflags = "#pragma\\s+uccm\\s+asflags\\s*\\+?=\\s*(.+)$".r
    val rRequire = "#pragma\\s+uccm\\s+require\\((\\w+)\\)\\s*\\+?=\\s*(.+)$".r
    val rFile    = "#pragma\\s+uccm\\s+file\\(([\\.\\-\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rFileEx  = "#pragma\\s+uccm\\s+file\\(([\\.\\-\\w]+)\\)\\s*\\~=\\s*(.+)$".r
    val rDefault = "#pragma\\s+uccm\\s+default\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rInfo    = "#pragma\\s+uccm\\s+info\\(([\\+\\.\\w]+)\\)\\s*=\\s*(.+)$".r
    val rDownload= "#pragma\\s+uccm\\s+download\\(([\\+\\.\\w]+)\\)\\s*=\\s*(.+)$".r
    val rInclude = "^#include\\s*<(uccm/./././[\\/\\.\\-\\+\\w]+)>\\s*$".r
    val rImport  = "^#include\\s*<~(\\w+)/(\\w+)/import.h>\\s*$".r
    val rLet     = "#pragma\\s+uccm\\s+let\\(([\\w]+)\\)\\s*=\\s*(.+)$".r
    val rLetIfNo = "#pragma\\s+uccm\\s+let\\(([\\w]+)\\)\\s*\\?=\\s*(.+)$".r

    def ns(s:String) = {
      val ss = s.dropRight(s.length-s.lastIndexWhere {' '.!= }-1)
      if ( ss.length >= 2 && ss.startsWith("\"") && ss.endsWith("\"") )
        ss.drop(1).dropRight(1)
      else
        ss
    }

    def parse(s:String): Option[Pragma] = s match {
      case rXcflags(tag,value) => Some(Xcflags(tag,ns(value)))
      case rBoard(tag,value) => Some(Board(tag,ns(value)))
      case rHome(tag,value)  => Some(Home(tag,ns(value)))
      case rAlias(tag,value) => Some(Alias(tag,ns(value)))
      case rCflags(value) => Some(Cflags(value))
      case rLdflags(value) => Some(Ldflags(value))
      case rAsflags(value) => Some(Asflags(value))
      case rRequire(tag,value) => Some(Require(tag,ns(value)))
      case rFile(tag,value) => Some(Append(tag,ns(value)))
      case rFileEx(tag,value) => Some(AppendEx(tag,ns(value)))
      case rDefault(tag,value) => Some(Default(tag,ns(value)))
      case rInfo(tag,value) => Some(Info(tag,ns(value)))
      case rDownload(tag,value) => Some(Download(tag,ns(value)))
      case rSoftDeviceAls(tag,value) => Some(SoftDeviceAls(tag,ns(value)))
      case rSoftDevice(tag,value) => Some(SoftDevice(tag,ns(value)))
      case rDebugger(tag,value) => Some(Debugger(tag,ns(value)))
      case rImport(accname,modname) => Some(Import(accname,modname))
      case rLet(tag,value) => Some(Let(tag,ns(value)))
      case rLetIfNo(tag,value) => Some(LetIfNo(tag,ns(value)))
      case _ => None
    }

    def qs(s: Stream[String]) : Stream[Option[Pragma]] = s match {
      case rInclude(fileName) #:: t =>
        qs(extractStrings(new File(BuildScript.uccmDirectoryFile,fileName))) append qs(t)
      case xs #:: t => parse(xs) #:: qs(t)
      case _ => Stream.empty
    }

    qs(js).filter{_.isDefined}.map{_.get}
  }
}
