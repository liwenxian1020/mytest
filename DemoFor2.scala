package com.itmumu

object DemoFor2 {
  def main(args: Array[String]): Unit = {
    for(i<- 1 to 3){
      for(j<- 1 to 5){
        print("*")
      }
      println()
    }

    for (i<- 1 to 9 if i%3==0){
      print(i+"\t")
    }
    println()

    for (i<-1 to 9;j<- 1 to 9 if i>=j){
      print(j+"*"+i+"="+(i*j)+"\t")
      if(i==j){
        println()
      }
    }
  }
}
