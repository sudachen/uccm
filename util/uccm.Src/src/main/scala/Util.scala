
package com.sudachen.uccm

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.zip.ZipFile

import scala.io.BufferedSource
import scala.collection.JavaConverters._
import scala.sys.process._

object Util {

  def bs(c:Char):Boolean = c match { case ' '|'\n'|'\r' => true case _ => false }
  def ns(s:String):String = s.dropWhile{bs}.reverse.dropWhile{bs}.reverse

  def quote(s: String): String = "\"" + s + "\""

  def winExePath(s: String): String = quote {
    s.map {
      case '/' => '\\'
      case '\"' => '\u0000'
      case c => c
    }.filter {
      '\u0000'.!=
    }
  }

  def posixPath(s: String): String = quote {
    s.map {
      case '\\' => '/'
      case '\"' => '\u0000'
      case c => c
    }.filter {
      '\u0000'.!=
    }
  }

  def unpackZip(file:File, dir:File, preproc: String => Option[String => String] = _ => None ) = {
    val zf = new ZipFile(file)
    try {
      zf.entries.asScala.foreach { r =>
        val f = new File(dir, r.getName)
        if (r.isDirectory)
          f.mkdirs()
        else {
          val is = zf.getInputStream(r)
          val os = new FileOutputStream(f)
          try {
            preproc(r.getName) match {

              case Some(p) =>

                val bs = new BufferedSource(is)
                val pw = new PrintWriter(os)
                try bs.getLines().foreach { s => pw.println(p(s)) } finally pw.flush()

              case None =>

                val bf = Array.ofDim[Byte](64 * 1024)

                def cpy(): Unit = is.read(bf) match {
                  case n if 0 <= n => os.write(bf, 0, n); cpy()
                  case _ =>
                }

                cpy()
            }
          } finally {
            os.close()
            is.close()
          }
        }
      }
    } finally {
      zf.close()
    }
  }

  def download(url:String, downloadDir:File, fName: Option[String] = None) : File = {

    val dst = fName match {
      case Some(n) => new File(downloadDir,n)
      case None => new File(downloadDir,new File(url).getName)
    }

    val dstSha1 = new File(dst.getPath+".sha1")
    val dstPart = new File(dst.getPath+".part")

    if (!downloadDir.exists) downloadDir.mkdirs()

    if ( !dst.exists || !dstSha1.exists ) {

      if ( dstPart.exists ) dstPart.delete()
      if ( dstSha1.exists ) dstSha1.delete

      val urlObj = new URL(url)
      System.out.println(s"connecting to ${urlObj.getHost} ...")
      val con = urlObj.openConnection().asInstanceOf[HttpURLConnection]
      try {
        val is = con.getInputStream
        val length = con.getContentLength
        val sha1 = java.security.MessageDigest.getInstance("SHA-1")
        val os = new FileOutputStream(dstPart)
        val bf = Array.ofDim[Byte](1024 * 1024)

        def cpy: Stream[Int] = is.read(bf) match {
          case -1 => Stream.empty
          case i => sha1.update(bf, 0, i); os.write(bf,0,i); i #:: cpy
        }

        try {
          System.out.print("downloading ")
          if ( length < 0 )
            System.out.print("... ")
          System.out.flush()
          val size = cpy.foldLeft(0) {
            (q, x) => BuildConsole.barUpdate(q,x,length)
          }
          System.out.print(s" success! $size")
        } finally {
          System.out.println()
          os.close()
        }

        dstPart.renameTo(dst)
        val sos = new FileWriter(dstSha1)

        try {
          sos.write(sha1.digest().map {
            "%02x".format(_)
          }.mkString + "\n")
        } finally {
          sos.close()
        }

      } finally {
        con.disconnect()
      }
    }
    dst
  }

  def copyFileToDir(dirFile:File, from:String,to:String,replace:String=>String): Unit = {

    val is = io.Source.fromFile(from)
    try {
      val os = new FileWriter(new File(dirFile, to))
      try
        is.getLines().foreach {
          s => os.write(replace(s)+"\n")
        }
      finally os.close()
    } finally is.close()

  }

  def mlink(from:File,to:File) : Int = {
    val fromWhere = from.getAbsolutePath.map { case '/' => '\\' case x => x }
    val toWhere = to.getAbsolutePath.map { case '/' => '\\' case x => x }
    val cmdl = List("cmd", "/c", "mklink", "/J", quote(toWhere), quote(fromWhere)).mkString(" ")
    BuildConsole.verbose(cmdl)
    val pl = ProcessLogger(s=>Unit,s=>println(s))
    cmdl!pl
  }

}