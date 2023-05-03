import Utils.startup
import akka.actor.typed.ActorSystem

import java.util.Scanner

object Main{
  def main(args: Array[String]): Unit = {

    val configuration = startup(args(0).toInt)
    ActorSystem[Nothing](Guard(), "Store", configuration)

    /*ActorSystem[Nothing](Guard(), "Store", startup(25252))
    ActorSystem[Nothing](Guard(), "Store", startup(25251))
    ActorSystem[Nothing](Guard(), "Store", startup(25253))*/

    val sc = new Scanner(System.in)
    sc.nextLine()
    sc.close()
  }
}