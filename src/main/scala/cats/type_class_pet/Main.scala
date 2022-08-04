package cats.type_class_pet
import Pet._

trait Pet[A] {
  def name(a: A): String
  def rename(a: A, newName: String): A
}


object Pet {
  implicit class PetSyntax[A](a: A)(implicit impl: Pet[A]) {
    def rename(newName: String): A = impl.rename(a, newName)
  }
}


case class Fish(name: String)
object Fish {
  implicit object RenameFish extends Pet[Fish] {
    def name(a: Fish): String = a.name
    def rename(a: Fish, newName: String): Fish = a.copy(newName)
  }
}


case class Dog(name: String)
object Dog {
  implicit object RenameDog extends Pet[Dog] {
    def name(a: Dog): String = a.name
    def rename(a: Dog, newName: String): Dog = a.copy(newName)
  }
}

object Main {
  /*
  * Via type classes we can add new functionality into the case classes
  * like `rename` method
  * */
  def main(args: Array[String]): Unit = {
    val fish = Fish("Boris").rename("Liza")
  }
}


