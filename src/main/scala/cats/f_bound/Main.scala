package cats.f_bound

object Main {
  def main(array: Array[String]): Unit = {

  }
}


// F-Bound polimorphism
trait Pets[A <: Pets[A]] { this: A =>
  def rename(str: String): A
}

case class Fish(name: String) extends Pets[Fish] {
  def rename(str: String): Fish = copy(str)
}

case class Dog(name: String) extends Pets[Dog] {
  def rename(str: String): Dog = copy(str)
}

class Mammut(name: String) extends Dog(name)
