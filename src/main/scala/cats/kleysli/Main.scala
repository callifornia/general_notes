package cats.kleysli
import cats.FlatMap
import cats.Functor
import cats.implicits._


object Main {

  /*
  *
  * flatMap is not a hard requirement
  * so basically we can implement with a map: but in this case Functor is required
  *
  * */
  case class Kleisli[F[_], In, Out](run: In => F[Out]) {
                                                                      // Kleisli[F[_], In, Out] current
    def andThen[Out2](k: Kleisli[F, Out, Out2])(implicit f: FlatMap[F]): Kleisli[F, In, Out2] =
      Kleisli[F, In, Out2](in => run.apply(in).flatMap(out => k.run(out)))

    // also we can do something like this where we put simple function: A => B
    def map[Out2](k: Out => Out2)(implicit f: Functor[F]): Kleisli[F, In, Out2] =
      Kleisli[F, In, Out2](in => run(in).map(out => k(out)))
  }


  def main(args: Array[String]): Unit = {
    /*
    * We do have:
    *   f: Int => String
    *   g: String => Double
    * So we can compose both functions in a way: f andThen g
    *
    *   h: String => Option[Int]
    *   p: Int => Option[Double]
    * We can not write: h andThen p, here is where Kleisli comes to play
    *
    * */
    val a: String => Option[Int] = _.toInt.some
    val b: Int => Option[Double] = _.toDouble.some
    val c: Int => Double         = _.toDouble

//    val c = a andThen b
    val cc: Kleisli[Option, String, Double] = Kleisli(a) andThen Kleisli(b)


    val dd = Kleisli(a).map(c)

    println("Result is: " + cc.run("123"))
    println("Result 2 is: " + dd.run("456"))
  }
}
