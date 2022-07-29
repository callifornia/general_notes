import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


trait AnotherObjectOne {

  trait GeneralMessage
  case class RequestFirstNumber(replyTo: ActorRef[Int]) extends GeneralMessage
  case class RequestLastFewNumbers(amount: Int, replyTo: ActorRef[Int]) extends GeneralMessage
  case class WrappedResult(result: Int, replyTo: ActorRef[Int]) extends GeneralMessage


  def streamInsidePart1(): Behavior[GeneralMessage] =
    Behaviors.setup[GeneralMessage] {context: ActorContext[GeneralMessage] =>

      implicit val m = Materializer.apply(context)
      implicit val ec = context.executionContext

      val stream = Source(1 to 10).via(Flow[Int].map(_ + 1))
      logic(stream, context)
    }


  def logic(source: Source[Int, NotUsed],
            context: ActorContext[GeneralMessage])
           (implicit m: Materializer, ec: ExecutionContext): Behavior[GeneralMessage] =

    Behaviors.receiveMessage {
      case RequestFirstNumber(replyTo) =>
        println("Going to calculate stream")
        val streamResult = source.runWith(Sink.head).map {el =>
          println("Received message hello: " + el)
          el
        }
        context.pipeToSelf(streamResult) {
          case Success(value) => WrappedResult(value, replyTo)
          case Failure(_) => WrappedResult(0, replyTo)
        }
        Behaviors.same[GeneralMessage]


      case RequestLastFewNumbers(amount, replyTo) =>
        println("Going to calculate stream")
        val streamResult = source.runWith(Sink.takeLast(amount)).map {el =>
          println("Received message another: " + el)
          el
        }
        context.pipeToSelf(streamResult) {
          case Success(value) => WrappedResult(value.sum, replyTo)
          case Failure(_) => WrappedResult(0, replyTo)
        }
        Behaviors.same[GeneralMessage]

      case WrappedResult(result, replyTo) =>
        replyTo ! result
        Behaviors.same
    }
}

object AnotherObjectOne extends AnotherObjectOne
