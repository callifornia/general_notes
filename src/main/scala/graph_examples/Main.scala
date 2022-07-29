package graph_examples

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{ClosedShape, FanInShape5, FlowShape, Graph, SourceShape, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, MergePrioritized, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.util.Timeout

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt


object Main {

  implicit val actorSystem: ActorSystem[String] = ActorSystem(Behaviors.empty[String], "asd")
  implicit val ex: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout = Timeout(3.seconds)


  def main(args: Array[String]): Unit = {
    Source(1 to 3).via(flowGraph).runWith(Sink.foreach(el => println("-----> " + el)))
  }


  /* zip | FlowGraph */
  def flowGraph =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))
        val zipper = builder.add(Zip[Int, Int]())
        val filter = Flow[Int].filter(_ != 3)

        broadcast.out(0).map(identity) ~> filter ~> zipper.in0
        broadcast.out(1)               ~>           zipper.in1

        FlowShape(broadcast.in, zipper.out)
      })


  /*
    zip | SourceGraph: in case one value is missed it's going to waite next elements

    (11,10001)
    (13,10002)   <-  12 was filtered out and result contains only 4 elements
    (14,10003)
    (15,10004)

    sourceGraph().runWith(Sink.foreach(el => println("----> " + el)))

  * */
  def sourceGraph(): Source[(Int, Int), NotUsed] =
    Source.fromGraph(GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._

      val source = Source(1 to 5)
      val zipper = builder.add(Zip[Int, Int]())

      source.map(_ + 10).filter(_ != 12) ~> zipper.in0
      source.map(_ + 10000) ~> zipper.in1

      SourceShape(zipper.out)
    })


  /* max with a for comprehension  */
  def pickMax(): RunnableGraph[Future[Done]] = {
    val sink = Sink.foreach[Int](el => println("----> " + el))
    RunnableGraph.fromGraph(GraphDSL.createGraph(sink) {implicit builder =>
      sink =>

        import GraphDSL.Implicits._
        val zipper: FanInShape5[Int, Int, Int, Int, Int, Int] =
          builder.add(ZipWith[Int, Int, Int, Int, Int, Int](
            (i1, i2, i3, i4, i5) =>
              (i1 :: i2 :: i3 :: i4 :: i5 :: Nil).max))

        (10 to 15)
          .map(Source.single)
          .zip(zipper.inlets)
          .foreach {
            case (index, in) => index ~> in.as[Int]
          }

        zipper.out ~> sink

        ClosedShape
    })

  }


  /* pick maximum of 3 source */
  def pickMaxOf3(): RunnableGraph[Future[Done]] = {

    import GraphDSL.Implicits._
    val sink = Sink.foreach[Int](el => println("-------> " + el))

    val pickMax3: Graph[UniformFanInShape[Int, Int], NotUsed] =
      GraphDSL.create() {implicit builder =>

        val zip1 = builder.add(ZipWith[Int, Int, Int]((in1, in2) => math.max(in1, in2)))
        val zip2 = builder.add(ZipWith[Int, Int, Int]((in1, in2) => math.max(in1, in2)))

        //  (N inputs, 1 output) which takes a function of N inputs that given a value for each input emits 1 output element

        zip1.out ~> zip2.in0

        UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
      }

    RunnableGraph.fromGraph(GraphDSL.createGraph(sink) {implicit builder =>
      sink =>

        val pickMax = builder.add(pickMax3)
        Source.single(11) ~> pickMax.in(0)
        Source.single(2) ~> pickMax.in(1)
        Source.single(3) ~> pickMax.in(2)

        pickMax.out ~> sink.in

        ClosedShape
    })
  }


  /* sequence of sink matched into the broadcasts */
  def firstGraph3(): RunnableGraph[Seq[Future[Done]]] = {
    val sink: Seq[Sink[Int, Future[Done]]] =
      (1 to 5)
        .map(_ => Sink.foreach[Int](el => println("custom stuff: " + el)))

    RunnableGraph.fromGraph(GraphDSL.create(sink) {implicit builder =>
      sinks =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Int](sinks.size))
        val source = Source(10 to 100)
        source ~> broadcast

        sinks.foreach(sink => broadcast ~> sink)
        ClosedShape
    })
  }


  /* put shared thinks into the graph */
  def firstGraph2() = {
    val sharedSink1: Sink[Int, Future[Done]] = Sink.foreach[Int](el => println("sharedSink1:  " + el))
    val sharedSink2: Sink[Int, Future[Done]] = Sink.foreach[Int](el => println("sharedSink2:  " + el))
    import GraphDSL.Implicits._
    RunnableGraph.fromGraph(GraphDSL.createGraph(sharedSink1, sharedSink2)((_, _)) {implicit builder =>
      (sharedSink1, sharedSink2) =>
        val in = Source(1 to 10)
        val f1 = Flow[Int].map(_ + 10)
        val f2 = Flow[Int].map(_ + 10000)
        val merge = builder.add(Merge[Int](2))
        val broadcast = builder.add(Broadcast[Int](2))

        in ~> f1 ~> merge ~> broadcast
        in ~> f2 ~> merge

        broadcast ~> sharedSink1
        broadcast ~> sharedSink2

        ClosedShape
    })
  }



  /* MergePreferred does not work in case broadcast in use */
  def firstGraph(): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._
      val in = Source(1 to 10)
      val in2 = Source(100 to 110)
      val out = Sink.foreach[Int](el => println("----------> " + el))

      val mergePreffered = builder.add(MergePreferred[Int](1))

      val f1 = Flow[Int].filter(_ > 0)
      val f2 = Flow[Int].map(_ + 0).map {el => (1 to 1000000).map(_ => ()); el}

      in2 ~> f1 ~> mergePreffered ~> out
      in ~> f2 ~> mergePreffered.preferred

      ClosedShape
    })


  def sleep() = Thread.sleep(3000)

}






















