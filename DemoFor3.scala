package com.itmumu

object DemoFor3 {
  def main(args: Array[String]): Unit = {
    val v =for (i<- 1 to 10) yield i*100
    println(v)
  }
}
