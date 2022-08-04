package cats.read_write_io

import cats.effect._
import java.io.{File, FileInputStream, FileOutputStream}
import cats.syntax.all._


object Main extends IOApp {


  def copy(source: File, destination: File): IO[Long] =
    (IO(new FileInputStream(source)), IO(new FileOutputStream(destination)))
      .tupled
      .bracket {
        case (in, out) => transfer(in, out)
      } {
        case (in, out) =>
          (IO(in.close()), IO(out.close())).tupled.handleErrorWith(_ => IO.unit).void
      }


  def transmit(source: FileInputStream, destination: FileOutputStream, buffer: Array[Byte], acc: Long): IO[Long] =
    for {
      amount <- IO.blocking(source.read(buffer, 0, buffer.size))
      count <- if (amount > -1) {
        IO.blocking(destination.write(buffer, 0, amount)) >> transmit(source, destination, buffer, acc + amount)
      } else IO.pure(acc)
    } yield count


  val transfer: (FileInputStream, FileOutputStream) => IO[Long] =
    (sourceFileName, destinationFileName) =>
      transmit(
        sourceFileName,
        destinationFileName,
        new Array[Byte](1024* 10),
        0L)


  def run(args: List[String]): IO[ExitCode] =
    for {
      _          <- IO.println("Please enter the source file name:")
      sourceFile <- IO.readLine
      _          <- IO.println("Please enter the destination file name: ")
      destFile   <- IO.readLine
      count      <- copy(new File(sourceFile), new File(destFile))
      _          <- IO.println("Amount of copied bytes: " + count)
    } yield ExitCode.Success
}
