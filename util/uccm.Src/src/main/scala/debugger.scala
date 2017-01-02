package com.sudachen.uccm.debugger

object Debugger extends Enumeration {
  val STLINK, JLINK = Value
  def fromString(name:String): Option[Value] = name match {
    case "stlink" => Some(STLINK)
    case "jlink" => Some(JLINK)
    case _ => None
  }

  def stringify(kind:Value):String = kind match {
    case JLINK => "jlink"
    case STLINK => "stlink"
  }
}
