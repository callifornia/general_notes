package actor_typed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.util.Timeout

import scala.concurrent.duration.DurationInt

object Main {

  trait SomeMessages
  case object MessageA extends SomeMessages
  case object MessageB extends SomeMessages
  case object MessageC extends SomeMessages

  // This way should be avoided as a we do have a `var` as a state
  val mutableState: Behavior[SomeMessages] = {
    Behaviors.setup[SomeMessages] { context =>
      var counter = 0
      Behaviors.receiveMessage {
        case MessageA =>
          println(s"!!!!! MessageA: {$counter}")
          counter = counter + 1
          Behaviors.same[SomeMessages]
        case MessageB =>
          println(s"!!!!! MessageB: {$counter}")
          counter = counter + 2
          Behaviors.same[SomeMessages]
        case MessageC =>
          println(s"!!!!! MessageC: {$counter}")
          counter = counter + 3
          Behaviors.same[SomeMessages]
      }
    }
  }

  // This way is preferable as it has no `var` but instead we do call method with an updated `counter`
  def immutableStateActor(counter: Int = 0): Behavior[SomeMessages] =
    Behaviors.receiveMessage[SomeMessages] {
      case MessageA =>
        println(s"!!!!! MessageA: {$counter}")
        immutableStateActor(counter + 1)
      case MessageB =>
        println(s"!!!!! MessageB: {$counter}")
        immutableStateActor(counter + 2)
      case MessageC =>
        println(s"!!!!! MessageC: {$counter}")
        immutableStateActor(counter + 3)
    }


  /*
  * Actor is a function:
  *   Message => Behavior[Message]
  *
  *   Behavior[Message]
  *     - can do side effect
  *     - can hold a state
  *     - can change the way of reacting on some messages
  *     -
  *
  * */

  import akka.actor.typed.ActorSystem
  import akka.actor.typed.ActorRef
  import akka.actor.typed.scaladsl.AskPattern._

  // example 2
  case class Hello(value: String)
  case class SayHello(name: String, replyTo: ActorRef[Hello])

  object FirstActor {
    def process(): Behavior[SayHello] =
      Behaviors.receiveMessage {
        case SayHello(name, replyTo) =>
          println("SayHello is going to reply ...")
          replyTo ! Hello(name + " hello from FirstActor")
          Behaviors.same
      }
  }


  def main(args: Array[String]): Unit = {
    implicit val timeout = Timeout(3.seconds)
    implicit val mutableStateActor = ActorSystem(immutableStateActor(), "sad")
    implicit val ec = mutableStateActor.executionContext

    val sys: ActorSystem[SayHello] = ActorSystem(FirstActor.process(), "SomeActorSystem")

    sys
      .ask(replyTo => SayHello("Jeremmy", replyTo))
      .foreach(result => println("Result is: " + result))

    Thread.sleep(2000)
    mutableStateActor.terminate()
  }
}
