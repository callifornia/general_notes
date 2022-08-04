package akka_actor_typed_stash_msg_adaptor_receptionist

import akka.actor.typed.receptionist.Receptionist.{Find, Listing, Register}
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors


object Main {

  def main(args: Array[String]): Unit = {
//    https://www.coursera.org/learn/scala-akka-reactive/lecture/CSQ9e/lecture-5-4-akka-typed-facilities
  }

  /*
  * Receptionist -> ability to find any actor in cluster, but first you need to register it.
  *
  * register it:
  *   val m2Actor = ctx.spawn(someOtherMessageBehavior(), "some-other-behavior")
  *   ctx.system.receptionist ! Register(ServiceKey[SomeOtherMessage]("some-other-message"), m2Actor)
  *
  * find it:
  *     val adaptor = ctx.messageAdapter(WrappedListing)
        ctx.system.receptionist ! Find(SomeOtherMessage.key, adaptor)
  *
  *   handle case when we get a response message:
  *       case WrappedListing(SomeOtherMessage.key.Listing(listings)) =>
              listings.foreach(otherMessageActor => otherMessageActor ! M2)
  *
  * */
  def receptionistStart(): Unit = {
    ActorSystem[Nothing](initReceptionist(), "foo")
    Thread.sleep(4000)
  }

  def initReceptionist(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      val m1Actor = ctx.spawn(someMessageBehavior(), "some-behavior")
      val m2Actor = ctx.spawn(someOtherMessageBehavior(), "some-other-behavior")
      ctx.system.receptionist ! Register(SomeMessage.key, m1Actor)
      ctx.system.receptionist ! Register(SomeOtherMessage.key, m2Actor)

      println("Init method ...")
      m1Actor ! M1
      Behaviors.same[Nothing]
    }


  def someMessageBehavior(): Behavior[SomeMessage] =
    Behaviors.receive { (ctx, msgs) =>
      msgs match {
        case M1 =>
          println("someMessageBehavior. Received M1 msg... going to fetch someOtherBehavior actor and send the message")
          val adaptor = ctx.messageAdapter(WrappedListing)
          ctx.system.receptionist ! Find(SomeOtherMessage.key, adaptor)
          Behaviors.same
        case WrappedListing(SomeOtherMessage.key.Listing(listings)) =>
          println("someMessageBehavior. Received WrappedListing, going to send the message into the SomeOtherMessage")
          listings.foreach(otherMessageActor => otherMessageActor ! M2)
          Behaviors.same
      }
    }


  def someOtherMessageBehavior(): Behavior[SomeOtherMessage] =
    Behaviors.receiveMessage {
      case M2 =>
        println("someOtherMessageBehavior recieved M2 message")
        Behaviors.same
    }

  trait SomeMessage
  object SomeMessage {
    val key = ServiceKey[SomeMessage]("some-message")
  }
  case object M1 extends SomeMessage
  case class WrappedListing(listing: Listing) extends SomeMessage

  trait SomeOtherMessage
  object SomeOtherMessage {
    val key = ServiceKey[SomeOtherMessage]("some-other-message")
  }

  case object M2 extends SomeOtherMessage





  /*
  * Stash/Unstash messages example
  *
  * */
  def runStashUnstash() = {
    val system = ActorSystem(stash(), "ActorSystem")
    1 to 28 foreach(el => system ! Second(el))
    system ! First
    Thread.sleep(4000)
  }

  def stash(): Behavior[StashMessage] =
    Behaviors.setup { ctx =>
      Behaviors.withStash(30) { buffer =>
        Behaviors.receiveMessagePartial {
          case First =>
            println("First ...")
            buffer.unstashAll(unstashBehavior())
            Behaviors.same
          case s: StashMessage =>
            println("Stash: " + s)
            buffer.stash(s)
            Behaviors.same
        }
      }
    }

  def unstashBehavior(): Behavior[StashMessage] =
    Behaviors.receiveMessage {
      case Second(id) =>
        println("unstashBehavior: " + id)
        Behaviors.same
    }


  trait StashMessage
  case object First extends StashMessage
  case class Second(id: Int) extends StashMessage

  /*
  * Examples below is about message adaptor.
  *
  * In case current actor does not support some type of message but
  * we are interesting on response (and father handling) to our self and we should provide a link to our self
  * we can use `ctx.messageAdaptor` which will return a link to OurSelf with a function inside
  * function inside it's just a wrapper on message response
  * */

  def init(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      println("Going to init stuff")
      val checkActor = ctx.spawn(check(), "check")
      val anotherActor = ctx.spawn(another(), "another")
      checkActor ! Something(anotherActor)
      Behaviors.stopped
    }


  def check(): Behavior[Message] = {
    Behaviors.receivePartial {
      case (ctx, Something(ref: ActorRef[AnotherMessageProtocol])) =>
        val adapter = ctx.messageAdapter(WrappedResponse)
        ref ! RequestSomething("foo", adapter)
        Behaviors.same
      case (ctx, WrappedResponse(response)) => Behaviors.same
    }
  }


  def another(): Behavior[AnotherMessageProtocol] =
    Behaviors.receivePartial {
      case (ctx, RequestSomething(id, replyTo)) =>
        println("Another check actor ..." + id)
        replyTo ! Response(id + "another")
        Behaviors.same
    }


  trait Message
  case class Something(ref: ActorRef[AnotherMessageProtocol]) extends Message
  case class WrappedResponse(rsp: Response) extends Message

  trait AnotherMessageProtocol
  case class Response(str: String)
  case class RequestSomething(id: String, replyTo: ActorRef[Response]) extends AnotherMessageProtocol
}
