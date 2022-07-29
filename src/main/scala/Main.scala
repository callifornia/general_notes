import akka.{Done, NotUsed}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, scaladsl}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import AnotherObjectOne._

/*
    * https://doc.akka.io/docs/akka/current/stream/index.html
    * Design Principles behind Akka Streams
    *
    * */


object Main {

  implicit val actorSystem: ActorSystem[GeneralMessage] = ActorSystem(streamInsidePart1(), "asd")
  implicit val ex: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout = Timeout(3.seconds)


  def main(args: Array[String]): Unit = {

    val namedSource: Source[Int, NotUsed] =
      Source(1 to 10)
        .map(_ + 1)
        .named("nestedSource")

    val namedFlow: Flow[Int, Int, NotUsed] =
      Flow[Int]
        .filter(_ != 0)
        .map(_ + 1)
        .named("nestedFlow")


    val nestedSink: Sink[Int, NotUsed] =
      namedFlow
        .to(Sink.fold(0)(_ + _))
        .named("nestedSink")


    val runnableGraph: RunnableGraph[NotUsed] = namedSource.to(nestedSink)


    sleep()
  }



  def runStreamInsideAnActor(): Future[Unit] = {
    actorSystem
      .ask(replyTo => RequestLastFewNumbers(3, replyTo))
      .map {result =>
        println("RequestLastFewNumbers result is: " + result)
      }

    actorSystem
      .ask(RequestFirstNumber)
      .map {result =>
        println("RequestFirstNumber result is: " + result)
      }
  }



  /* everything before .async will be occurred in one actor and the rest (after .async) in another */
  def runInParallel(): Future[Done] =
    Source(1 to 10)
      .map{el => println("first One: " + el); el}
      .async
      .map{el => println("second one: " + el); el}
      .runForeach(_ => ())


  def sinkIntoTwoSink(): Sink[Int, NotUsed] =
    Flow[Int]
      .alsoTo(Sink.foreach(el => println("Share here: " + el)))
      .to(Sink.foreach(el => println(s"Also here: " + el)))


  def sleep(in: Int = 20000): Unit = Thread.sleep(in)
}
