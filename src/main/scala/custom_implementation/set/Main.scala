package own_set

import scala.annotation.tailrec


object Main {
  def main(args: Array[String]): Unit = {

    val mySet = MySet(1,2,3)
    val mySet2 = MySet(2, 3,6,7,8)

//    mySet intersection mySet2 foreach println

    mySet diff mySet2 foreach println

//    mySet + 5 foreach println


//    (for {
//      el <- mySet
//      el2 <- mySet2
//    } yield (el , el2)) foreach println


//    mySet ++ mySet2 foreach println

//    mySet filter (_ == 2) foreach println

  }
}


trait MySet[A] extends (A => Boolean) {
  def -(el: A): MySet[A]
  def intersection(another: MySet[A]): MySet[A]
  def diff(another: MySet[A]): MySet[A]
  def apply(v1: A): Boolean = contains(v1)
  def +(el: A): MySet[A]
  def ++(els: MySet[A]): MySet[A]
  def contains(el: A): Boolean
  def map[B](f: A => B): MySet[B]
  def flatMap[B](f: A => MySet[B]): MySet[B]
  def filter(condition: A => Boolean): MySet[A]
  def foreach(predicate: A => Unit): Unit
}


object MySet {
  def apply[A](elements: A*): MySet[A] = {

    @tailrec
    def build(seq: Seq[A], acc: MySet[A]): MySet[A] =
      if(seq.isEmpty) acc
      else build(seq.tail, acc + seq.head)

    build(elements, new EmptySet[A])
  }
}


class EmptySet[A] extends MySet[A] {
  def contains(el: A): Boolean = false
  def -(el: A): MySet[A] = this
  def intersection(another: MySet[A]): MySet[A] = this
  def diff(another: MySet[A]): MySet[A] = this
  def +(el: A): MySet[A] = new NonEmptySet[A](el, this)
  def ++(els: MySet[A]): MySet[A] = els
  def map[B](f: A => B): MySet[B] = new EmptySet[B]
  def flatMap[B](f: A => MySet[B]): MySet[B] = new EmptySet[B]
  def filter(condition: A => Boolean): MySet[A] = this
  def foreach(predicate: A => Unit): Unit = ()
}

class NonEmptySet[A](head: A, tail: MySet[A]) extends MySet[A] {
  def contains(el: A): Boolean = (el == head) || tail.contains(el)

  def -(el: A): MySet[A] =
    if(head == el) tail
    else tail - el + head

  def intersection(another: MySet[A]): MySet[A] = {
    if(another.contains(head)) another.intersection(tail) + head
    else another.intersection(tail)
  }

  def diff(another: MySet[A]): MySet[A] =
    if(another.contains(head)) another.diff(tail)
    else another.diff(tail) + head

  def +(el: A): MySet[A] =
    if(contains(el)) this
    else new NonEmptySet[A](el, this)

  def ++(another: MySet[A]): MySet[A] = tail ++ another + head
  def map[B](f: A => B): MySet[B] = tail.map(f) + f(head)
  def flatMap[B](f: A => MySet[B]): MySet[B] = tail.flatMap(f) ++ f(head)

  def filter(predicate: A => Boolean): MySet[A] =
    if(predicate(head)) tail.filter(predicate) + head
    else tail.filter(predicate)

  def foreach(condition: A => Unit): Unit = {
    condition(head)
    tail.foreach(condition)
  }
}
