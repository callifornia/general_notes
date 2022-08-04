package cats.effect
import cats.effect._
import cats.effect.implicits._

object Main extends IOApp.Simple {

//  val run = for {
//    _ <- IO.println("Hello there")
//    _ <- IO.println("How are you ?")
//  } yield ()

  val run = IO{println("Hello there"); throw new Exception("asd") } >> IO.println("How are you ?")


}
