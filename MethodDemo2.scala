package com.itmumu

object MethodDemo2 {
  def main(args: Array[String]): Unit = {
    defaultParm("xiaoda")
  }
def defaultParm(name:String,gender:String="nan"): Unit ={
  print(s"姓名=${name},性别=${gender}")
 }
}
