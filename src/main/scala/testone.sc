import scala.util.Random
val array = Seq("l", ";", "j", "k")

val result = {
  val numberOfRow = (1 to 10) map { _ =>
    val row = (1 to 10) map {_ =>
      val el = (1 to 5).map {_ =>
        array(Random.nextInt(array.length))
      }
      el.mkString
    }
    row.mkString(" ")
  }
  numberOfRow.mkString("\n")
}
result