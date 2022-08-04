package zio.something_else

object Main {
  def main(array: Array[String]): Unit = {

  }



  case class InputOutputFunctionality[A](inputOutputFunction: () => A) { self =>
    // map
    def instructionToExecuteInputOutputFunctionAndChangeResult[B](f: A => B): InputOutputFunctionality[B] = {
      val aValue: A = self.inputOutputFunction.apply() // ExecuteInputOutputFunction
      val bValue: B = f(aValue)                        // ChangeResult
      InputOutputFunctionality(() => bValue)
    }

    // flatMap
    def instructionToExecuteInputOutputFunctionChangeResultExecuteAnotherInputOutputFunctionChangeResult[B](f: A => InputOutputFunctionality[B]): InputOutputFunctionality[B] = {
      val aValue: A = self.inputOutputFunction.apply() // ExecuteInputOutputFunction
      val bIO: InputOutputFunctionality[B] = f(aValue) // ChangeResult
      val bValue: B = bIO.inputOutputFunction.apply()  // ExecuteAnotherInputOutputFunction
      InputOutputFunctionality(() => bValue)
    }
  }
}
