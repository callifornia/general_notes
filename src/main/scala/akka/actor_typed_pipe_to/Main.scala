package akka.actor_typed_pipe_to

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.Future
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import akka.actor.typed.ActorSystem

object Main {

  trait SomeMessage
  case class FindPhone(name: String) extends SomeMessage
  case class PersonPhone(number: Int) extends SomeMessage
  case class PhoneNotFound(name: String, reason: Any) extends SomeMessage

  object ExternalService {
    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(3))
    private val db: Map[String, Int] = Map("a" -> 123, "b" -> 456)

    def findPhone(name:String): Future[Int] = Future(db(name))
  }

  // PipeTo example as an actor
  def someActor(counter: Int = 0, failure: Int = 0): Behavior[SomeMessage] = {
    Behaviors.receive {
      (context, message) =>
        message match {
          case FindPhone(name) =>
            context.pipeToSelf(ExternalService.findPhone(name)) {
              case Success(phone) => PersonPhone(phone)
              case Failure(exception) => PhoneNotFound(name, exception)
            }
            Behaviors.same
          case PersonPhone(number) =>
            println(s"Founded persons number: [$number]")
            someActor(counter + 1, failure)
          case PhoneNotFound(name, reason) =>
            println(s"Persons [$name] number was not found. Reason: [$reason]")
            someActor(counter, failure + 1)
        }
    }
  }


  // PipeTo example as a function
  val someActor2: (Int, Int) => Behavior[SomeMessage] = {
    (counter, failure) => {
      Behaviors.receive[SomeMessage] {
        (context, message) =>
          message match {
            case FindPhone(name) =>
              context.pipeToSelf(ExternalService.findPhone(name)) {
                case Success(phone) => PersonPhone(phone)
                case Failure(exception) => PhoneNotFound(name, exception)
              }
              Behaviors.same
            case PersonPhone(number) =>
              println(s"Founded persons number: [$number]")
              someActor2(counter + 1, failure)
            case PhoneNotFound(name, reason) =>
              println(s"Persons [$name] number was not found. Reason: [$reason]")
              someActor2(counter, failure + 1)
          }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val blahActor = ActorSystem(someActor2(0, 0), "asd")

    1 to 10 foreach { counter =>
      blahActor ! FindPhone("a")
      blahActor ! FindPhone("b")
      blahActor ! FindPhone("c")
      println(s"Messages: [${counter * 3}] were sent")
    }

    Thread.sleep(30000)
    blahActor.terminate()
  }
}
