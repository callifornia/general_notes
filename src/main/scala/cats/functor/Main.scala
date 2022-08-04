package functor

object Main {

  // ############################################################################################################
  /*
  * A Functor for a type provides the ability for its values to be "mapped over",
  * i.e. apply a function that transforms inside a value while remembering its shape.
  * For example, to modify every element of a collection without dropping or adding elements.
  *
  *
  * Laws:
  *   fa:Functor[F[_]]
  *   identity: fa.map(x => x) == fa
  *   compsotion: fa.map(f).map(g) == fa.map(x => f(x) andThen g(x))
  *
  * */

  trait CustomFunctor[C[_]] {
    def map[A, B](container: C[A])(function: A => B): C[B]
  }

  // implicit case class
  extension [C[_], A, B](container: C[A])(using functor: CustomFunctor[C])
    def map(function: A => B): C[B] = functor.map(container)(function)

  // specific implementation for `CustomFunctor[Tree]`
  given treeFunctor: CustomFunctor[Tree] with
    def map[A, B](container: Tree[A])(function: A => B): Tree[B] =
      container match {
        case Leaf(value) => Leaf(function(value))
        case Branch(value, left, right) => Branch(function(value), map(left)(function), map(right)(function))
      }

  // ############################################################################################################


  // Example data structure
  trait Tree[+T]

  case class Leaf[+T] private (value: T) extends Tree[T]
  object Leaf {
    def apply[T](value: T): Leaf[T] = new Leaf(value)
  }
  case class Branch[+T] private (value: T, left: Tree[T], right: Tree[T]) extends Tree[T]
  object Branch {
    def apply[T](value: T, left: Tree[T], right: Tree[T]): Branch[T] = new Branch(value, left, right)
  }



  val tree: Tree[Int] =
    Branch(
      value = 1,
      left =
        Branch(
          value = 2,
          left =
            Branch(
              value = 3,
              left = Leaf(4),
              right = Leaf(5)),
          right = Leaf(6)),
      right = Leaf(7))


  @main def run(): Unit = {
    println(tree.map(_ * 10))
  }
}
