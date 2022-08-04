package custom_implementation.monad

object Main {
  trait CMonad[+A] {
    def apply[A](value: A): CMonad[A] = CMonadImpl(value)
    def flatMap[B](function: A => CMonad[B]): CMonad[B]
    def map[B](function: A => B): CMonad[B]
    def withFilter(condition: A => Boolean): CMonad[A]
  }

  object CMonad {
    def apply[A](value: A): CMonad[A] = CMonadImpl(value)
  }

  case class CMonadImpl[A](value: A) extends CMonad[A] { self =>
    def flatMap[B](function: A => CMonad[B]): CMonad[B] = function(value)
    def map[B](function: A => B): CMonad[B] = CMonadImpl(function(value))
    def withFilter(condition: A => Boolean): CMonad[A] =
      if (condition(value)) self
      else CMonadEmpty

  }

  case object CMonadEmpty extends CMonad[Nothing] {
    def flatMap[B](function: Nothing => CMonad[B]): CMonad[B] = CMonadEmpty
    def map[B](function: Nothing => B): CMonad[B] = CMonadEmpty
    def withFilter(condition: Nothing => Boolean): CMonad[Nothing] = CMonadEmpty
  }


  def main(args: Array[String]): Unit = {
    val a = CMonad(123)
    val b = CMonad(456)

    val result = for {
      aa <- CMonad(123)
      bb <- CMonad(456) if bb == 6
    } yield aa + bb

    println("result is: " + result)
  }
}

