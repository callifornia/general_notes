package zio.layers

import zio._
import zio.console.{Console, getStrLn, putStrLn}
import zio.clock.Clock
import zio.App


object Main extends App {
  def run(args: List[String]) = {
    program().exitCode
  }


  def program() =
    (for {
      _         <- putStrLn("Are you going to add something ?")
      a         <- getStrLn.map(_.toInt)
      _         <- putStrLn("You have entered: " + a)
      b         <- getStrLn.map(_.toInt)
      _         <- putStrLn("You have entered: " + b)
      result    <- Calculator.add(a, b)
      _         <- Logger.log("The result is: " + result.toString)
    } yield ()).provideLayer(LoggerImpl.layer >+> Console.live >+> CalculatorImpl.layer)


  def programSmallPart() =
    (for {
      _       <- putStrLn("EnterSomething")
      input   <- getStrLn
      _       <- Logger.log("Something: " + input)
    } yield ()).provideLayer(LoggerImpl.layer >+> Console.live)
}


trait Calculator extends Serializable {
  def add(a: Int, b: Int): ZIO[Any, Exception, Int]
}

// a Calculator API
object Calculator {
  def add(a: Int, b: Int) = ZIO.serviceWith[CalculatorImpl](_.add(a, b))
}

// a Calculator implementation
case class CalculatorImpl(logger: LoggerImpl, console: Console.Service) extends Calculator {
  def add(a: Int, b: Int): ZIO[Any, Exception, Int] =
    for {
      _      <- logger.log("Going to add numbers")
      result <- ZIO.effect(a + b).catchAll(_ => ZIO.fail(new ArithmeticException("Can not add few numbers")))
      _      <- logger.log("Calculation result is: " + result)
    } yield result
}

// a Calculator implementation as a layer
object CalculatorImpl {
  val layer: URLayer[Has[LoggerImpl] with Has[Console.Service], Has[CalculatorImpl]] =
    (CalculatorImpl(_, _)).toLayer
}





trait Logger extends Serializable {
  def log(str: String): UIO[Unit]
}

// an interface for outer world
object Logger {
  def log(str: String) = ZIO.serviceWith[LoggerImpl](_.log(str))
}

case class LoggerImpl(console: Console.Service) extends Logger {
  override def log(str: String): UIO[Unit] =
    for {
      _ <- console.putStrLn(str).orDie
    } yield ()
}

// express layer
object LoggerImpl {
  val layer: URLayer[Has[Console.Service], Has[LoggerImpl]] = (LoggerImpl(_)).toLayer
}
