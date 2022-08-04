package zio.new_way_of_general_programing_writing

object Main {
  def main(array: Array[String]): Unit = {

    sealed trait Console[+A]
    final case class Return[A](value: () => A) extends Console[A]
    final case class PrintLine[A](line: String, rest: Console[A]) extends Console[A]
    final case class ReadLine[A](rest: String => Console[A]) extends Console[A]

    implicit class ConsoleSyntax[A](self: Console[A]) {
      def map[B](f: A => B): Console[B] = flatMap(a => Return(() => f(a)))

      def flatMap[B](f: A => Console[B]): Console[B] =
        self match {
          case Return(value) => f(value.apply())
          case PrintLine(line, rest) => PrintLine(line, rest.flatMap(f))
          case ReadLine(stringToConsoleA) =>
            ReadLine(anotherString => stringToConsoleA(anotherString).flatMap(f))
        }
    }


    def printLine(line: String): Console[Unit] = PrintLine(line, Return(() => ()))

    def readLine: Console[String] = ReadLine(line => Return(() => line))

    val program: Console[Unit] =
      PrintLine(
        "Please type something ...",
        ReadLine(
          input =>
            PrintLine(
              s"You have entered ...: $input", Return(() => ()))))


    val program2 =
      for {
        _ <- printLine("Enter something interesting")
        line <- readLine
        _ <- printLine("You have entered an: " + line)
      } yield ()


    def interpreter[A](program: Console[A]): A =
      program match {
        case Return(value) => value()
        case ReadLine(rest) => interpreter(rest(scala.io.StdIn.readLine()))
        case PrintLine(line, rest) =>
          println(line)
          interpreter(rest)
      }

    interpreter(program2)
  }
}
