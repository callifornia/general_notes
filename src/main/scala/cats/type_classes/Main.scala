package type_classes

object Main {
  case class ScoredPoint(amount: Amount)
  case class Amount(value: Int)
  case class Description(value: String)



  trait Json[T] {
    def toJson(value: T): JsonValue
  }

  implicit class JsonSyntax[T](value: T) {
    def toJson(implicit converter: Json[T]): JsonValue = converter.toJson(value)
  }

  implicit object IntJson extends Json[Int] {
    def toJson(value: Int): JsonValue = IntJsonValue(value)
  }
  implicit object StringJson extends Json[String] {
    def toJson(value: String): JsonValue = StringJsonValue(value)
  }

  implicit object DescriptionJson extends Json[Description] {
    def toJson(value: Description): JsonValue =
      JsonObjectValue(Map("description" -> StringJsonValue(value.value)))
  }
  implicit object AmountJson extends Json[Amount] {
    def toJson(value: Amount): JsonValue = JsonObjectValue(
      Map(
        "amount" -> JsonObjectValue(Map(
          "value" -> value.value.toJson))
      )
    )
  }

  implicit object ScoredPoint extends Json[ScoredPoint] {
    def toJson(value: ScoredPoint): JsonValue =
      JsonObjectValue(Map(
        "scoredPoint" -> value.amount.toJson)
      )
  }

  trait JsonValue {
    def stringify: String
  }

  case class IntJsonValue(value: Int) extends JsonValue {
    def stringify: String = value.toString
  }

  case class StringJsonValue(value: String) extends JsonValue {
    def stringify: String = "\"" + value + "\""
  }

  case class JsonObjectValue(value: Map[String, JsonValue]) extends JsonValue {
    def stringify: String =
      value.map {
        case (key, value) => "\"" + key + "\":" + "" + value.stringify + ""
      }.mkString("{", ",", "}")
  }

  def main(args: Array[String]): Unit = {
    println(Description("some descrids").toJson.stringify)
    println(Amount(123).toJson.stringify)
    println(ScoredPoint(Amount(345)).toJson.stringify)
    /*
    * Type class is the thing which provide sme interface for some kind of defined types
    * */
  }
}

