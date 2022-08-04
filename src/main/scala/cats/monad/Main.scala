package cats.monad

object Main {
  def main(args: Array[String]): Unit = {
    // https://doc.akka.io/docs/akka/current/typed/actors.html
    // https://docs.scala-lang.org/scala3/reference/contextual/extension-methods.html
    //https://manuel.bernhardt.io/2019/10/07/tour-of-akka-typed-event-sourcing/
    CustomMonad("foo")
      .flatMap(v => CustomMonad(v.toUpperCase))
      .flatMap(v => CustomMonad(v.toLowerCase)) :: Nil foreach println



  }
}

/*
  Extract -> Transform -> Wrap

  Monad has two fundamental operation
  1. Wrap a value: class `CustomMonad` wrap `value`. In functional world it's named as a `pure` or `unit`
  2. Transform a value by the given function, in our case it's a `flatMap`: T => CustomMonad[S]

  Monad(x).flatMap(f) == f(x)                                                    left identity
  Monad(x).flatMap(x => Monad(x)) == Monad(x)                                    right identity (USELESS)
  Monad(x).flatMap(f).flatMap(g) == Monad(x).flatMap(x => f(x).flatMap(g))       composition, associativity (ETW -> ETW -> ETW)
 */
case class CustomMonad[+T](private val value: T ) {
  def get(): T = value
  def flatMap[S](function: T => CustomMonad[S]): CustomMonad[S] =
    function(value)
}