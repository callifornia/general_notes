package zio.run_main

import zio.CanFail.canFailAmbiguous1
import zio.{App, ExitCode, URIO, ZEnv, ZIO}
import zio.console._

import java.io.IOException
import java.time.Instant
import scala.concurrent.Future
import scala.io.AnsiColor
import Future._


object Main extends App {
  def run(args: List[String]) =
    (for {
      result <- ZIOErrorHandling.program()
      result <- ZIOErrorHandling.program2()
      _ <- putStrLn(result.toString)
    } yield ()).exitCode
}


object ZIOErrorHandling {
  def program() =
    ZIO
      .effect{
        if (false) throw new Exception("sdaa")
        else "asd"
      }
      .foldM(
        error => ZIO.effect(123),
        (success: String) => ZIO.effect(456))


  def program2() =
    ZIO
      .effect{
        if (true) throw new Exception("sdaa")
        else "asd"
      }
      .fold(
        error => 123,
        (success: String) => 456
      )
}


object ZIOEither {
  def program() = {
    ZIO.fromEither(Right("right value"))
    ZIO.fromEither(Left("some error"))
  }
}



object ZIOEffect {

  def program() = ZIO.effect(println("Wrapped zio"))

  program
}



sealed trait AppError
case object SomeError extends AppError

object SomeFuture {
  lazy val someFuture = Future.successful {
    println("PrintFromFuture")
    if (true) {
      println("Goint to throw an exception")
      throw new Exception("bloh bloh")
    }
    else 123
  }

  val program = ZIO.fromFuture { implicit ex =>
    someFuture.map(n => println("Print from the ZIO future" + n))
  }.catchAll{
    case ex: Exception =>
      putStrLn("Catched some exception")
  }.orElse(putStrLn("Something else"))

  program.exitCode
}
