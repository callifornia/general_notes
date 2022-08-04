package algoritm.linked.list

object Main {
  def main(args: Array[String]): Unit = {
    val linkedList = LinkedList.apply(1)
    val dd: LinkedList[Int] = linkedList :+ 2 :+ 3 :+ 4 :+ 5
    println(dd)
    println(dd.head)
    println(dd.tail)
    println(dd.tail.head)
    println(dd.last)

    val ff: LinkedList[Int] = 99 +: dd
    println(ff)
    println(ff.map(_ + 1))
    ff.map(_ + 1).foreach(print)
  }
}


sealed trait LinkedList[+A] {
  def isEmpty: Boolean
  def last: Option[A]
  def head: Option[A]
  def tail: LinkedList[A]
  def :+ [B >: A](value: B): LinkedList[B]
  def +: [B >: A](value: B): LinkedList[B]
  def map[B](f: A => B): LinkedList[B]
  def foreach(f: A => Unit): Unit
}
object LinkedList {
  def apply[A](value: A): LinkedList[A] = new Node(value, End)
}


case class Node[+A](value: A, next: LinkedList[A]) extends LinkedList[A] {
  def last: Option[A] = if(next.isEmpty) Some(value) else next.last
  def isEmpty: Boolean = false
  def tail: LinkedList[A] = next
  def head: Option[A] = Some(value)
  def :+ [B >: A](value: B): LinkedList[B] = this.copy(next = next :+ value)
  def +:[B >: A](value: B): LinkedList[B] = Node(value, this)
  def map[B](f: A => B): LinkedList[B] = Node(f(value), next.map(f))
  def foreach(f: A => Unit): Unit = {
    f(value)
    next.foreach(f)
  }
}


object End extends  LinkedList[Nothing] {
  def isEmpty: Boolean = true
  def head: Option[Nothing] = None
  def last: Option[Nothing] = None
  def tail: LinkedList[Nothing] = End
  def :+ [B >: Nothing](value: B): LinkedList[B] = LinkedList.apply(value)
  def +:[B >: Nothing](value: B): LinkedList[B] = LinkedList.apply(value)
  def map[B](f: Nothing => B): LinkedList[B] = End
  def foreach(f: Nothing => Unit): Unit = ()

  override def toString: String = "End"
}
