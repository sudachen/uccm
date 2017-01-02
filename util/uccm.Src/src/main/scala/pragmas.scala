
package com.sudachen.uccm.pragmas

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
case class UccmDefault(tag:String, value:String) extends UccmPragma
case class UccmInfo(tag:String, value:String) extends UccmPragma
case class UccmDownload(tag:String, value:String) extends UccmPragma

object Pragmas {
  def extractFrom(file:File) : Stream[UccmPragma] = {
    val f = io.Source.fromFile(file)

    def js(s: Stream[String]) : Stream[String] = s match  {
      case xs #:: t if xs.endsWith("\\") => (xs.dropRight(1) + js(t).head) #:: js(t).tail
      case xs #:: t => xs #:: js(t)
      case _ => f.close; Stream.empty
    }

    val rXcflags = "#pragma\\s*uccm\\s*xcflags\\(([\\*\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rBoard   = "#pragma\\s*uccm\\s*board\\(([\\*\\w]+)\\)\\s*=\\s*(.+)$".r
    val rHome    = "#pragma\\s*uccm\\s*home\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rAlias   = "#pragma\\s*uccm\\s*alias\\(([A-Z_0-9]+)\\)\\s*=\\s*(.+)$".r
    val rCflags  = "#pragma\\s*uccm\\s*cflags\\s*\\+?=\\s*(.+)$".r
    val rLdflags = "#pragma\\s*uccm\\s*ldflags\\s*\\+?=\\s*(.+)$".r
    val rAsflags = "#pragma\\s*uccm\\s*asflags\\s*\\+?=\\s*(.+)$".r
    val rRequire = "#pragma\\s*uccm\\s*require\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rFile    = "#pragma\\s*uccm\\s*file\\(([\\.\\-\\w]+)\\)\\s*\\+?=\\s*(.+)$".r
    val rDefault = "#pragma\\s*uccm\\s*default\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rInfo    = "#pragma\\s*uccm\\s*info\\((\\w+)\\)\\s*=\\s*(.+)$".r
    val rDownload= "#pragma\\s*uccm\\s*download\\((\\w+)\\)\\s*=\\s*(.+)$".r

    def ns(s:String) = {
      val ss = s.dropRight(s.length-s.lastIndexWhere { _ !=  ' ' }-1)
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
      case rDefault(tag,value) => Some(UccmDefault(tag,ns(value)))
      case rInfo(tag,value) => Some(UccmInfo(tag,ns(value)))
      case rDownload(tag,value) => Some(UccmDownload(tag,ns(value)))
      case _ => None
    }

    def qs(s: Stream[String]) : Stream[Option[UccmPragma]] = s match {
      case xs #:: t => parse(xs) #:: qs(t)
      case _ => Stream.empty
    }

    qs(js(f.getLines().toStream)).filter{_.isDefined}.map{_.get}
  }
}
