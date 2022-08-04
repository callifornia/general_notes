package algoritm.binary.tree


object Main {
  sealed trait BinaryTree[+A] {
    def :+[B >: A](value: B): BinaryTree[B]
    def isEmpty: Boolean
    def size: Int
    def sum[B >: A](implicit op: BinaryTreeOperations[B, BinaryTree]): B = op.summing(this)
  }

  object BinaryTree {
    def apply[A](value: A): BinaryTree[A] = Node(value, End, End)
  }


  case class Node[A](item: A, leftSide: BinaryTree[A], rightSide: BinaryTree[A]) extends BinaryTree[A] {

    def size: Int = 1 + leftSide.size + rightSide.size
    def isEmpty: Boolean = false
    def :+[B >: A](value: B): BinaryTree[B] = {
      if (item == value) this
      else if (leftSide.size < rightSide.size) this.copy(leftSide = leftSide :+ value)
      else if (leftSide.size > rightSide.size) this.copy(rightSide = rightSide :+ value)
      else this.copy(leftSide = leftSide :+ value)
    }
  }

  case object End extends BinaryTree[Nothing] {
    def size: Int = 0
    def :+[B >: Nothing](value: B): BinaryTree[B] = BinaryTree.apply[B](value)
    def isEmpty: Boolean = true
  }

  sealed trait BinaryTreeOperations[A, BinaryTree[_]] {
    def summing(tree: BinaryTree[A]): A
  }

  object BinaryTreeOperationsImpl {
    implicit object BinaryTreeSumInt extends BinaryTreeOperations[Int, BinaryTree] {
      def summing(tree: BinaryTree[Int]): Int = tree match {
        case Node(item, leftSide, rightSide) => item + summing(leftSide) + summing(rightSide)
        case End => 0
      }
    }

    implicit object BinaryTreeSumString extends BinaryTreeOperations[String, BinaryTree] {
      def summing(tree: BinaryTree[String]): String = tree match {
        case Node(item, leftSide, rightSide) => item + " " + summing(leftSide) + " "+ summing(rightSide)
        case End => ""
      }
    }
  }

  import BinaryTreeOperationsImpl._
  def main(args: Array[String]): Unit = {
    val bt = BinaryTree(1)
    val newBt: BinaryTree[Int] = BinaryTree(1) :+ 2 :+ 3 :+ 4 :+ 5 :+ 6
    println(newBt)
    println(newBt.size)
    println(newBt.sum)

    println((BinaryTree("a") :+ "b" :+ "c").sum)
  }
}



