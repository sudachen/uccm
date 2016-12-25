package uccm
import java.io.File

case class CmdlOptions(release: Boolean = true,
                       debug: Boolean = false,
                       reconfig: Boolean = false,
                       reset: Boolean = false,
                       erase: Boolean = false,
                       flash: Boolean = false,
                       connect: Boolean = false,
                       mainFile: Option[File] = None)

object Uccm {

  def main(argv: Array[String]): Unit = {
    val cmdlParser = new scopt.OptionParser[CmdlOptions]("uccm") {
      head("uC cortex-m build manager", "1.0")
      help("help").
        text("show this help and exit")

      override def showUsageOnError = true

      opt[Unit]("reconfig").
        action( (_,c) => c.copy(reconfig = true)).
        text("reconfigure build")

      opt[Unit]("release").
        action( (_,c) => c.copy(debug = false, release = true)).
        text("build release")

      opt[Unit]("debug").
        action( (_,c) => c.copy(debug = true, release = false)).
        text("build debug")

      opt[Unit]("erase").
        action( (_,c) => c.copy(erase = true)).
        text("erase mCu memory")

      opt[Unit]("flash").
        action( (_,c) => c.copy(flash = true)).
        text("write firmware into mCu memory")

      opt[Unit]("reset").
        action( (_,c) => c.copy(reset = true)).
        text("reset mCu")

      opt[Unit]("connect").
        action( (_,c) => c.copy(connect = true)).
        text("start debugger connected to mCu")

      arg[File]("main.c").optional().
        action( (x,c) => c.copy(mainFile = Some(x))).
        text("firmware main.c file")
    }

    cmdlParser.parse(argv, CmdlOptions()) match {
      case Some(cmdlOpts) => act(cmdlOpts)
      case None =>
    }
  }

  def act(cmdlOpts: CmdlOptions) : Unit = {

  }
}