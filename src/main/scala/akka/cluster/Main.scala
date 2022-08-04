package akka.cluster

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.empty, "actor-system-custom-name")
    println("Please enter some key ....")
    StdIn.readLine()
    system.terminate()
  }
}
