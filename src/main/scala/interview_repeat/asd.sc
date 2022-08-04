import cats.FlatMap
import cats.implicits._
/*
*
* val a = String => Option[Int]
* val b = Int    => Option[Boolean]
*
* */
case class Kleisly[F[_], In, Out](run: In => F[Out]) {
  def andThen[Out2](g: Kleisly[F, Out, Out2])(implicit flatMap: FlatMap[F]): Kleisly[F, In, Out2] =
    Kleisly[F, In, Out2](in => run.apply(in).flatMap(out => g.run(out)))

  def andThen[Out2](g: Out => F[Out2])(implicit flatMap: FlatMap[F]): Kleisly[F, In, Out2] =
    Kleisly[F, In, Out2](in => run(in).flatMap(out => g(out)))
}

val a: String => Option[Int] = str => {
  println("String => Option[Int]")
  Some(str.toInt)
}
val b: Int    => Option[Boolean] = number => {
  println("Int => Option[Boolean]")
  Some(number.isValidInt)
}

val aa: Kleisly[Option, String, Int] = Kleisly(a)
val bb: Kleisly[Option, Int, Boolean] = Kleisly(b)


val someFunction = aa andThen b
val someFunction2 = aa andThen bb

val r1 = someFunction.run("1")
val r2 = someFunction.run("2")
val r3 = someFunction2.run("4")
//someFunction.run("as")


