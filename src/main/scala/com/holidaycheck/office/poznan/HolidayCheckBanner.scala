package com.holidaycheck.office.poznan

object HolidayCheckBanner extends App {
  val width = 52
  val label = "HolidayCheck"

  case class A(dir: Int, pos: Int)

  def nextA(a: A): A = {
    if (a.pos < width && a.pos > 0) a.copy(pos = a.pos + a.dir)
    else a.copy(dir = a.dir * -1, pos = a.pos - a.dir)
  }

  def go(a: A): Unit = {
    printA(a)
    Thread sleep 50
    go(nextA(a))
  }

  def printA(a: A): Unit = println {
    (0 to width).foldLeft("") {
      (line, i) => if (i == a.pos) s"$line$label" else s"$line "
    }
  }

  go(A(-1, 0))
}