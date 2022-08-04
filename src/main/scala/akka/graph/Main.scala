package akka.graph_examples

import akka.{Done, NotUsed}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{ActorAttributes, ActorMaterializer, ActorMaterializerSettings, Attributes, ClosedShape, FanInShape5, FlowShape, Graph, Inlet, Materializer, Outlet, SourceShape, Supervision, UniformFanInShape}
import akka.stream.scaladsl.{Balance, Broadcast, Compression, FileIO, Flow, GraphDSL, Merge, MergePreferred, MergePrioritized, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.{ByteString, Timeout}
import scala.concurrent.duration._
import java.lang.IndexOutOfBoundsException
import java.nio.file.Paths
import scala.concurrent.{ExecutionContextExecutor, Future, blocking}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random


object Main {

  implicit val actorSystem: ActorSystem[String] = ActorSystem(Behaviors.empty[String], "asd")
  implicit val ex: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout = Timeout(3.seconds)


  def main(args: Array[String]): Unit = {
    IosAndroidNotificationMechanism.graph().run()
    Thread.sleep(10000)
  }


  object Test {
    def graph() =
      Source(1 to 10)
        .groupedWithin(3, 3 seconds)
        .runWith(Sink.foreach(println))
  }



  object IosAndroidNotificationMechanism {
    trait MobileMsg {
      def id: Int = Random.nextInt(1000)
      def toGenMsg(origin: String): GenericMsg = GenericMsg(id, origin)
    }
    class AndroidMsg extends MobileMsg
    class IosMsg extends MobileMsg
    case class GenericMsg(id: Int, origin: String)


    val statefulCounterFlow: GraphStage[FlowShape[Seq[GenericMsg], Int]] =
      new GraphStage[FlowShape[Seq[GenericMsg], Int]] {
        val in: Inlet[Seq[GenericMsg]] = Inlet[Seq[GenericMsg]]("SomeInlet")
        val out: Outlet[Int] = Outlet[Int]("CustomOutlet")

        override def shape: FlowShape[Seq[GenericMsg], Int] = FlowShape(in, out)
        override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
          new GraphStageLogic(shape) {
            var counter = 0
            setHandler(in, new InHandler {
              override def onPush(): Unit = {
                val element = grab(in)
                counter += element.size
                push(out, counter)}})

            setHandler(out, new OutHandler {
              override def onPull(): Unit = pull(in)
            })
          }
      }


    def graph(): RunnableGraph[NotUsed] =
      RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val sourceAndroid = Source.tick(1 seconds, 500 millis, new AndroidMsg)
        val sourceIos = Source.tick(700 millis, 600 millis, new IosMsg)
        def counterSink(name: String): Sink[Int, Future[Done]] = Sink.foreach[Int](el => println(s"==$name==> " + el))

        val toGenMsgAndGroupAndroid: Flow[AndroidMsg, Seq[GenericMsg], NotUsed] = Flow[AndroidMsg].map(_.toGenMsg("ANDROID")).groupedWithin(5, 5 seconds).async
        val toGenMsgAndGroupIos: Flow[IosMsg, Seq[GenericMsg], NotUsed]         = Flow[IosMsg].map(_.toGenMsg("IOS")).groupedWithin(5, 5 seconds).async
        val counter: Flow[Seq[GenericMsg], Int, NotUsed] = Flow.fromGraph[Seq[GenericMsg], Int, NotUsed](statefulCounterFlow)
        val mapper         = Flow[Seq[GenericMsg]].mapConcat(_.toList)

        val broadCastAndroid = builder.add(Broadcast[Seq[GenericMsg]](2))
        val broadCastIos = builder.add(Broadcast[Seq[GenericMsg]](2))
        val balancer = builder.add(Balance[Seq[GenericMsg]](2))

        val mergeSeq = builder.add(Merge[Seq[GenericMsg]](2))
        val merge = builder.add(Merge[GenericMsg](2))
        def log(n: String): Flow[Seq[GenericMsg], Seq[GenericMsg], NotUsed] =
          Flow[Seq[GenericMsg]].map{el => println(s"----$n----> " + el); el}

        def log2(n: String): Flow[AndroidMsg, AndroidMsg, NotUsed] =
          Flow[AndroidMsg].map {el => println(s"----$n----> " + el); el}

        sourceAndroid ~> toGenMsgAndGroupAndroid ~> broadCastAndroid ~> counter ~> counterSink("Android")
        
                                                    broadCastAndroid ~> mergeSeq
                                                                                                       balancer ~> mapper ~> merge
                                                                                 mergeSeq ~> balancer
                                                                                                                                   merge ~> Sink.foreach(println)
                                                                                                       balancer ~> mapper ~> merge
                                                    broadCastIos     ~> mergeSeq

        sourceIos     ~> toGenMsgAndGroupIos     ~> broadCastIos ~> counter ~> counterSink("IOS")

        ClosedShape
      })
  }




  /* count the words */
  object StatefulElementCounterGraph {

    case class Counter(value: Int) {
      def increment: Counter = Counter(value + 1)
    }
    object Counter {
      val empty = Counter(0)
    }

    val statefulCounterFlow: Flow[Int, (Int, Counter), NotUsed] =
      Flow.fromGraph(
        new GraphStage[FlowShape[Int, (Int, Counter)]] {
          val in: Inlet[Int] = Inlet.apply[Int]("CustomInlet")
          val out: Outlet[(Int, Counter)] = Outlet[(Int, Counter)]("CustomOutlet")

          override def shape: FlowShape[Int, (Int, Counter)] = FlowShape(in, out)
          override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =

            new GraphStageLogic(shape) {
              var counter = Counter.empty

              setHandler(in, new InHandler {
                override def onPush(): Unit = {
                  val element = grab(in)
                  println("StatefulCounterFlow.inHandler: " + element)
                  counter = counter.increment
                  push(out, (element, counter))}})

              setHandler(out, new OutHandler {
                override def onPull(): Unit = {
                  pull(in)}})}})



    def graph(): RunnableGraph[NotUsed] =
      Source(500 to 1000)
        .via(statefulCounterFlow)
        .to(Sink.foreach(el => println("---> " + el)))
  }


  /*
  Case when all flows are independed (flow0, flow1, flow2 ... )
      -- balance.out(0):  --> 0
      -- balance.out(1):  --> 1
      -- balance.out(2):  --> 2
      -- balance.out(3):  --> 3
      -- balance.out(4):  --> 4

      washing 0
      washing 1
      washing 2
      washing 3
      washing 4

      -- merge.in(0) --> 0
      -- merge.in(1) --> 1
      -- merge.in(2) --> 2
      -- merge.in(3) --> 3
      -- merge.in(4) --> 4

      ==> 0
      ==> 1
      ==> 2
      ==> 3
      ==> 4


  Case when all flows are the same with a some timedelay
  Balance[Int](5, true)
  flow.async

      -- balance.out(2):  --> 0
      -- balance.out(1):  --> 1
      -- balance.out(3):  --> 2
      -- balance.out(0):  --> 3
      -- balance.out(4):  --> 4
      washing 10000
      washing 10000
      washing 10000
      washing 10000
      washing 10000

  Case when all flows are the same with a some timedelay
    Balance[Int](5, false)
    flow.async
    everything will go into the one balance output
    -- balance.out(1):  --> 0
    -- balance.out(1):  --> 1
    -- balance.out(1):  --> 2
    -- balance.out(1):  --> 3
    -- balance.out(1):  --> 4
    washing 10000

  Case when all flows are the same with a some timedelay
    Balance[Int](5, false)
    flow without async
    will go into the each output port one by one. When first element will be in the FINISH ONLY then next one is going
    to be consumed by the flow

    --  balance.out(0):  --> 0
      washing 10000
    --  merge.in(0) --> 0
    ==> 0

    --  balance.out(1):  --> 1
        washing 10000
    --  merge.in(1) --> 1
    ==> 1
  *
  * */
  object GraphBalancer {
    val wash0 =
      Flow[Int]
        .map {el =>
          println("washing " + el)
          el
        }

    val wash1 =
      Flow[Int]
        .map {el =>
          println("washing " + el)
          el
        }
    val wash2 =
      Flow[Int]
        .map {el =>
          println("washing " + el)
          el
        }

    val wash3 =
      Flow[Int]
        .map {el =>
          println("washing " + el)
          el
        }

    val wash4 =
      Flow[Int]
        .map {el =>
          val timeDelay = 10000
          println("washing " + timeDelay)
          Thread.sleep(timeDelay)
          el
        }

    def graph(): RunnableGraph[NotUsed] =
      RunnableGraph.fromGraph(GraphDSL.create() {implicit builder =>
        import GraphDSL.Implicits._

        val source = Source(0 to 4)
        val sink = Sink.foreach[Int](el => println("==>" + el))
        val balancer = builder.add(Balance[Int](5, true))
        val merge = builder.add(Merge[Int](5))

        def log(n: String): Flow[Int, Int, NotUsed] = Flow[Int].map {el => println(s"-- $n --> " + el); el}

        source ~> balancer.in
        balancer.out(0) ~> log("balance.out(0): ") ~> wash0 ~> log("merge.in(0)") ~> merge.in(0)
        balancer.out(1) ~> log("balance.out(1): ") ~> wash1 ~> log("merge.in(1)") ~> merge.in(1)
        balancer.out(2) ~> log("balance.out(2): ") ~> wash2 ~> log("merge.in(2)") ~> merge.in(2)
        balancer.out(3) ~> log("balance.out(3): ") ~> wash3 ~> log("merge.in(3)") ~> merge.in(3)
        balancer.out(4) ~> log("balance.out(4): ") ~> wash4 ~> log("merge.in(4)") ~> merge.in(4)
        merge.out ~> sink

        ClosedShape
      })
  }


  /* broadcast just copy elements into the all there output ports */
  object GraphBroadcast {

    def graph() =
      RunnableGraph.fromGraph(GraphDSL.create() {implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[Int](4))
        val merge = builder.add(Merge[Int](4))
        val source = Source(1 to 5)
        val sink = Sink.foreach[Int](el => println(s"==> $el"))

        def log(n: String): Flow[Int, Int, NotUsed] = Flow[Int].map {el => println(s"--$n--> " + el); el}

        source ~> broadcast.in
        broadcast.out(0) ~> log("b0") ~> merge.in(0)
        broadcast.out(1) ~> log("b1") ~> merge.in(1)
        broadcast.out(2) ~> log("b2") ~> merge.in(2)
        broadcast.out(3) ~> log("b3") ~> merge.in(3)
        merge.out ~> sink
        ClosedShape
      })
  }

  object StreamWithAnActor {

    sealed trait SomeMessages

    case object InitMessage extends SomeMessages

    val firstActor: Behavior[SomeMessages] =
      Behaviors.receive {
        case (ctx, InitMessage) =>
          ctx.log.info("First actor received the message: " + InitMessage)
          Behaviors.same[SomeMessages]
      }

    val secondsActor: Behavior[SomeMessages] =
      Behaviors.receive {
        case (ctx, InitMessage) =>
          ctx.log.info("Second actor received some message")
          Behaviors.same[SomeMessages]
      }


    def runGraph =
      RunnableGraph.fromGraph(GraphDSL.create() {implicit builder =>
        ???
      })
  }


  /*

    run in terminal:
      echo -n "1 1 2 3" | netcat 127.0.0.1 1234

    */
  object TCPConnection {

    private val connection =
      Tcp()(actorSystem.classicSystem)
        .bind("127.0.0.1", 1234)

    private val flow =
      Flow[ByteString]
        .map(_.utf8String.toUpperCase)
        .map {el =>
          println("Received the word: " + el)
          el
        }
        .mapConcat(_.split(" ").toList)
        .filter(_.nonEmpty)
        .collect {case w if w.nonEmpty =>
          w.replaceAll("""[p{Punct}&&[^.]]""",
            "").replaceAll(System.lineSeparator(), "")
        }
        .groupBy(100, identity)
        .map(el => (el, 1))
        .mergeSubstreams
        .map {
          case (word, counter) => ByteString.apply(word + " -> " + counter + "\n")
        }

    def runTCPConnection(): Unit = {
      connection.runForeach(connection =>
        connection.handleWith(flow))
    }
  }


  /*
      SyncAsyncParallelRunningStuff.runSynchronus().run()
      SyncAsyncParallelRunningStuff.runAsynchroniouse().run()
      SyncAsyncParallelRunningStuff.runParrallel().run()
  */
  object SyncAsyncParallelRunningStuff {

    case class Wash(id: Int)

    case class Dry(id: Int)

    case class Done(id: Int)


    private def wash(): Flow[Wash, Dry, NotUsed] =
      Flow[Wash]
        .map {wash =>
          val takesTime = 1000
          println(s"Washing(${wash.id}) it will take: " + takesTime + "s")
          Thread.sleep(takesTime)
          Dry(wash.id)
        }


    private def dry(): Flow[Dry, Done, NotUsed] =
      Flow[Dry]
        .map {dry =>
          val takesTime = 16000
          println(s"Drying(${dry.id}) it will take: " + takesTime + "s")
          Thread.sleep(takesTime)
          Done(dry.id)
        }


    private def parallelGraph: Flow[Wash, Done, NotUsed] =
      Flow.fromGraph(GraphDSL.create() {implicit builder =>
        import GraphDSL.Implicits._
        val balancer = builder.add(Balance[Wash](3))
        val merge = builder.add(Merge[Done](3))
        val printLog1: String => Flow[Wash, Wash, NotUsed] =
          n => Flow[Wash].map {el => println(s"----> " + n); el}

        balancer.out(0) ~> printLog1("b0") ~> wash().async ~> dry().async ~> merge.in(0)
        balancer.out(1) ~> printLog1("b1") ~> wash().async ~> dry().async ~> merge.in(1)
        balancer.out(2) ~> printLog1("b2") ~> wash().async ~> dry().async ~> merge.in(2)

        FlowShape(balancer.in, merge.out)
      })


    private def runGraph(flow: Flow[Wash, Done, NotUsed]): RunnableGraph[NotUsed] =
      Source(Wash(1) :: Wash(2) :: Wash(3) :: Wash(4) :: Wash(5) :: Nil)
        .map {el => println(s"bb: $el"); el}
        .via(flow)
        .to(Sink.foreach(println))


    /*
       One by one through  all process flow
       encapsulates the execution within a single actor
       Only when the element is successfully processed by the Sink, another element is pulled from the Source
    */
    def runSynchronus(): RunnableGraph[NotUsed] =
      runGraph(Flow[Wash].via(wash()).via(dry()))

    /* as soon Washing(1) done -> Washing(2) will start immediately and until the end, the same with a Drying
    *  Next element do not need to wait until previouse one will finish his line till the end of the flow
    *
    * */
    def runAsynchroniouse(): RunnableGraph[NotUsed] =
      runGraph(Flow[Wash].via(wash().async).via(dry().async))

    def runParrallel(): RunnableGraph[NotUsed] =
      runGraph(Flow[Wash].via(parallelGraph))
  }


  object ErrorHandling {
    val streamErrorHandle: Supervision.Decider = {
      case _: IndexOutOfBoundsException =>
        println("IndexOutOfBoundsException exception: ")
        Supervision.Resume
      case _: IllegalArgumentException =>
        println("IllegalArgumentException exception: ")
        Supervision.Resume
      case _ => Supervision.Stop
    }

    def apply(): Future[Done] =
      Source(1 to 10)
        .map {
          case 2 => throw new IndexOutOfBoundsException()
          case 5 => throw new IllegalArgumentException()
          case n: Int => n
        }
        .withAttributes(ActorAttributes.withSupervisionStrategy(ErrorHandling.streamErrorHandle.apply))
        .runWith(Sink.foreach(println))
  }


  /*
      Source
        .fromGraph(HelloAkkaStreamWorld.infinityStringSource)
        .runWith(Sink.foreach(println))
  */
  object HelloAkkaStreamWorld {
    val infinityStringSource: GraphStage[SourceShape[String]] =
      new GraphStage[SourceShape[String]] {
        val out: Outlet[String] = Outlet.apply("SourceOut")

        override def shape: SourceShape[String] = SourceShape.apply(out)

        override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
          new GraphStageLogic(shape) {
            setHandler(out, new OutHandler {
              override def onPull(): Unit = {
                val line = "Something hello"
                push(out, line)
              }
            })
          }
      }
  }


  /*
      List(1, 2, 2, 3, 2)
      groupBy(Int.MaxValue, identity)
      ....

      current: (2,1)
      next:    (2,1)

      current: (2,2)
      next:    (2,1)

      (2,3)
      (3,1)
      (1,1)
   */
  def countRepeatedWords(): RunnableGraph[NotUsed] =
    Source(List(1, 2, 2, 3, 2))
      .groupBy(100, identity)
      .map(_ -> 1)
      .reduce {(current, next) =>
        println(s"""
                   | current: $current
                   | next: $next
                   |""".stripMargin)
        (current._1, current._2 + next._2)
      }
      .mergeSubstreams
      .to(Sink.foreach(e => println(e)))


  /* merge two Sink */
  def mergeSinks() = {
    val sink1 = Sink.foreach[Int](el => println("sink1: " + el))
    val sink2 = Sink.foreach[Int](el => println("sink2: " + el))

    val combinedThinks = Sink.combine(sink1, sink2)(Broadcast[Int](_))

    Source(1 to 10).runWith(combinedThinks)

  }

  /* merge two Sources */
  def mergeSources(): Future[Done] = {
    val source1 = Source(1 to 10)
    val source2 = Source(100 to 110)
    val flow: Source[Int, NotUsed] = Source.combine(source1, source2)(el => Merge(el))

    flow.runWith(Sink.foreach(el => println(el)))
  }


  /* zip | FlowGraph */
  def flowGraph =
    Flow.fromGraph(
      GraphDSL.create() {implicit builder =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))
        val zipper = builder.add(Zip[Int, Int]())
        val filter = Flow[Int].filter(_ != 3)

        broadcast.out(0).map(identity) ~> filter ~> zipper.in0
        broadcast.out(1) ~> zipper.in1

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


  def sleep(): Unit = Thread.sleep(3000)

}
