
package com.sudachen.uccm

import java.io.File

class UccmPragma
case class UccmAlias(tag:String, value:String) extends UccmPragma
case class UccmHome(tag:String, value:String) extends UccmPragma
case class UccmBoard(tag:String, value:String) extends UccmPragma
case class UccmXcflags(tag:String, value:String) extends UccmPragma
case class UccmCflags(value:String) extends UccmPragma
case class UccmLdflags(value:String) extends UccmPragma
case class UccmAsflags(value:String) extends UccmPragma
case class UccmRequire(tag:String,value:String) extends UccmPragma
case class UccmAppend(tag:String, value:String) extends UccmPragma
case class UccmAppendEx(tag:String, value:String) extends UccmPragma
case class UccmDefault(tag:String, value:String) extends UccmPragma
case class UccmInfo(tag:String, value:String) extends UccmPragma
case class UccmDownload(tag:String, value:String) extends UccmPragma
case class UccmSoftDevice(tag:String, value:String) extends UccmPragma
case class UccmSoftDeviceAls(tag:String, value:String) extends UccmPragma
case class UccmDebugger(tag:String, value:String) extends UccmPragma
case class UccmImport(accname:String, modname:String) extends UccmPragma

object Pragmas {

  def extractFromTempFile(tempFile:File) : Stream[UccmPragma] = {
    def js(s: Stream[UccmPragma]) : Stream[UccmPragma] = s match {
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

  def extractFrom(file:File) : Stream[UccmPragma] = {
    def js: Stream[String] = extractStrings(file)

    val rXcflags = "#pragma\\s*uccm\\s*xcflags\\(([\\*\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rDebugger= "#pragma\\s*uccm\\s*debugger\\(([\\*\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rBoard   = "#pragma\\s*uccm\\s*board\\(([\\*\\w]+)\\)\\s*=\\s*(.+)$".r
    val rSoftDevice  = "#pragma\\s*uccm\\s*softdevice\\(([\\+\\w]+)\\)\\s*=\\s*(.+)$".r
    val rSoftDeviceAls  = "#pragma\\s*uccm\\s*softdevice\\(([\\+\\w]+)\\)\\s*=>\\s*(.+)$".r
    val rHome    = "#pragma\\s*uccm\\s*home\\(([\\+\\.\\w]+)\\)\\s*=\\s*(.+)$".r
    val rAlias   = "#pragma\\s*uccm\\s*alias\\(([A-Z_0-9]+)\\)\\s*=\\s*(.+)$".r
    val rCflags  = "#pragma\\s*uccm\\s*cflags\\s*\\+?=\\s*(.+)$".r
    val rLdflags = "#pragma\\s*uccm\\s*ldflags\\s*\\+?=\\s*(.+)$".r
    val rAsflags = "#pragma\\s*uccm\\s*asflags\\s*\\+?=\\s*(.+)$".r
    val rRequire = "#pragma\\s*uccm\\s*require\\((\\w+)\\)\\s*\\+?=\\s*(.+)$".r
    val rFile    = "#pragma\\s*uccm\\s*file\\(([\\.\\-\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rFileEx  = "#pragma\\s*uccm\\s*file\\(([\\.\\-\\w]+)\\)\\s*\\~=\\s*(.+)$".r
    val rDefault = "#pragma\\s*uccm\\s*default\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rInfo    = "#pragma\\s*uccm\\s*info\\(([\\+\\.\\w]+)\\)\\s*=\\s*(.+)$".r
    val rDownload= "#pragma\\s*uccm\\s*download\\(([\\+\\.\\w]+)\\)\\s*=\\s*(.+)$".r
    val rInclude = "^#include\\s*<(uccm/./././[\\/\\.\\-\\+\\w]+)>\\s*$".r
    val rImport  = "^#include\\s*<~(\\w+)/(\\w+)/import.h>\\s*$".r

    def ns(s:String) = {
      val ss = s.dropRight(s.length-s.lastIndexWhere {' '.!= }-1)
      if ( ss.length >= 2 && ss.startsWith("\"") && ss.endsWith("\"") )
        ss.drop(1).dropRight(1)
      else
        ss
    }

    def parse(s:String): Option[UccmPragma] = s match {
      case rXcflags(tag,value) => Some(UccmXcflags(tag,ns(value)))
      case rBoard(tag,value) => Some(UccmBoard(tag,ns(value)))
      case rHome(tag,value)  => Some(UccmHome(tag,ns(value)))
      case rAlias(tag,value) => Some(UccmAlias(tag,ns(value)))
      case rCflags(value) => Some(UccmCflags(value))
      case rLdflags(value) => Some(UccmLdflags(value))
      case rAsflags(value) => Some(UccmAsflags(value))
      case rRequire(tag,value) => Some(UccmRequire(tag,ns(value)))
      case rFile(tag,value) => Some(UccmAppend(tag,ns(value)))
      case rFileEx(tag,value) => Some(UccmAppendEx(tag,ns(value)))
      case rDefault(tag,value) => Some(UccmDefault(tag,ns(value)))
      case rInfo(tag,value) => Some(UccmInfo(tag,ns(value)))
      case rDownload(tag,value) => Some(UccmDownload(tag,ns(value)))
      case rSoftDeviceAls(tag,value) => Some(UccmSoftDeviceAls(tag,ns(value)))
      case rSoftDevice(tag,value) => Some(UccmSoftDevice(tag,ns(value)))
      case rDebugger(tag,value) => Some(UccmDebugger(tag,ns(value)))
      case rImport(accname,modname) => Some(UccmImport(accname,modname))
      case _ => None
    }

    def qs(s: Stream[String]) : Stream[Option[UccmPragma]] = s match {
      case rInclude(fileName) #:: t =>
        qs(extractStrings(new File(BuildScript.uccmDirectoryFile,fileName))) append qs(t)
      case xs #:: t => parse(xs) #:: qs(t)
      case _ => Stream.empty
    }

    qs(js).filter{_.isDefined}.map{_.get}
  }
}
