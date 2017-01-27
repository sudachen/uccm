package com.sudachen.uccm

object BuildConsole {

  var useColors = false
  var beVerbose = false

  def panic(text :String) :Unit = {
    System.out.println(panic_prefix+text+color_suffix)
    System.exit(1)
  }

  def error(text :String) :Unit = {
    System.out.println(panic_prefix+text+color_suffix)
  }

  def info(text :String) :Unit = {
    System.out.println(info_prefix+text+color_suffix)
  }

  def verbose(text :String) :Unit = {
    if ( beVerbose )
      System.out.println(verbose_prefix + text + color_suffix)
  }

  def verboseInfo(text :String) :Unit = {
    if ( beVerbose )
      System.out.println(info_prefix + text + color_suffix)
  }

  def barUpdate(old:Int,append:Int,total:Int) : Int = {
    if ( total > 0 ) {
      val step = Math.max(total / 50, 1)
      val dif = (old + append) / step - old / step
      if (dif > 0) {
        System.out.print("#" * dif)
        System.out.flush()
      }
    }
    old + append
  }

  def stackTrace(bt: Array[StackTraceElement]) :Unit = {
    if ( beVerbose )
      bt.foreach { x =>
        System.out.println(verbose_prefix + x.toString + color_suffix)
      }
  }

  def panicBt(text: String, bt: Array[StackTraceElement]) :Unit = {
    if ( beVerbose )
      bt.take(9).foreach { x =>
        System.out.println(verbose_prefix + x.toString + color_suffix)
      }
    System.out.println(panic_prefix+text+color_suffix)
    System.exit(1)
  }

  def panic_prefix :String = (if ( useColors ) Console.RED else "") + "[!] "
  def info_prefix :String = (if ( useColors ) Console.GREEN else "") + "[>] "
  def verbose_prefix :String = if (useColors) "\u001B[38m" else ""
  def color_suffix :String = if (useColors) Console.RESET else ""

}