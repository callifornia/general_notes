package interview_repeat
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.util.Timeout
import akka.persistence.typed.PersistenceId
import scala.concurrent.duration.DurationInt
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.persistence.typed.scaladsl.Effect
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.receptionist.Receptionist.{Find, Listing, Register}
import akka.actor.typed.receptionist.ServiceKey
import akka.persistence.typed.scaladsl.EventSourcedBehavior


object Main {
  def main(args: Array[String]): Unit = {

  }
}

/*
*                       Type classes
*
*
* Type class is the thing which provide some interface for some kind of defined types
*
*
*
*
* String                 -> JsonValue
* Int                    -> JsonValue
* Map[String, JsonValue] -> JsonValue
*
* */
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


/* case classes */
case class ScoredPoint(amount: Amount)
case class Amount(value: Int)
case class Description(value: String)


object toJsonWrapper {

  /* main interface */
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

  /* usage */

  Description("some description").toJson.stringify


  /*
  *
  *   Another example:  Via type classes we can add new functionality into the case classes
  *
  * */

  trait Pet[A] {
    def rename(a: A, newName: String): A
  }

  case class Fish(name: String)
  object Fish {
    implicit object RenameFish extends Pet[Fish] {
      def rename(a: Fish, newName: String): Fish = a.copy(newName)
    }
  }

  // sharing for user
  object Pet {
    implicit class PetSyntax[A](a: A)(implicit impl: Pet[A]) {
      def rename(newName: String): A = impl.rename(a, newName)
    }
  }
  import Pet._
  Fish("Boris").rename("Liza")




/*
*                               Monad
*
*  Extract -> Transform -> Wrap
*  1. Wrap a value: class `CustomMonad` wrap `value`. In functional world it's named as a `pure` or `unit`
*  2. Transform a value by the given function, in our case it's a `flatMap`: T => CustomMonad[S]
*
*
*  Laws:
*  Monad(x).flatMap(f) == f(x)                                                  left identity
*  Monad(x).flatMap(x => Monad(x)) == Monad(x)                                  right identity (USELESS)
*  Monad(x).flatMap(f).flatMap(g) == Monad(x).flatMap(x => f(x).flatMap(g))     composition | associativity
*
* */

  case class CustomMonad[+T](private val value: T) {
    def get(): T = value

    def flatMap[S](function: T => CustomMonad[S]): CustomMonad[S] =
      function(value)
  }


                                  /* Custom for-comprehension */

  trait CMonad[+A] {
    def apply[A](value: A): CMonad[A] = CMonadImpl(value)
    def flatMap[B](function: A => CMonad[B]): CMonad[B]
    def map[B](function: A => B): CMonad[B]
    def withFilter(condition: A => Boolean): CMonad[A]
  }

  object CMonad {
    def apply[A](value: A): CMonad[A] = CMonadImpl(value)
  }

  case class CMonadImpl[A](value: A) extends CMonad[A] { self =>
    def flatMap[B](function: A => CMonad[B]): CMonad[B] = function(value)
    def map[B](function: A => B): CMonad[B] = CMonadImpl(function(value))
    def withFilter(condition: A => Boolean): CMonad[A] = if (condition(value)) self else CMonadEmpty
  }

  case object CMonadEmpty extends CMonad[Nothing] {
    def flatMap[B](function: Nothing => CMonad[B]): CMonad[B] = CMonadEmpty
    def map[B](function: Nothing => B): CMonad[B] = CMonadEmpty
    def withFilter(condition: Nothing => Boolean): CMonad[Nothing] = CMonadEmpty
  }

  // usage:

  for {
    aa <- CMonad(123)
    bb <- CMonad(456) if bb == 6 // for if we should implement `def withFilter`
  } yield aa + bb




/*
*                               Functor
*
* A Functor for a type provides the ability for its values to be "mapped over",
* i.e. apply a function that transforms inside a value while remembering its shape.
* For example, to modify every element of a collection without dropping or adding elements.
*
*
* Laws:
*   fa:Functor[F[_]]
*   fa.map(x => x)   == fa                                identity
*   fa.map(f).map(g) == fa.map(x => f(x) andThen g(x))    composition
*
* */


  /* main trait */
  trait CustomFunctor[C[_]] {
    def map[A, B](container: C[A])(function: A => B): C[B]
  }

  /* Extension for everyone */
  object CustomFunctor {
    implicit class CustomFunctorSyntax[A, B, C[_]](container: C[A])(implicit functor: CustomFunctor[C]) {
      def map(function: A => B): C[B] = functor.map(container)(function)
    }
  }

  case class Foo[A](value: A)
  object Foo {
    /* concrete implementation */
    implicit object FooFunctorImpl extends CustomFunctor[Foo] {
      def map[A, B](container: Foo[A])(function: A => B): Foo[B] =
        Foo(function(container.value))
    }
  }

  /* usage */
  import CustomFunctor._
  Foo(123).map(_ + 2)



/*
*                           F-Bound polymorphism
*
* F-bounded polymorphism (a.k.a self-referential types, recursive type signatures, recursively bounded quantification)
* is a powerful object-oriented technique that leverages the type system to encode constraints on generics
*
* это мощный объектно-ориентированный метод, который использует систему типов для кодирования ограничений на дженерики.
* */

  trait Pets[A <: Pets[A]] { this: A =>
    def rename(str: String): A
  }

  case class Fish2(name: String) extends Pets[Fish2] {
    def rename(str: String): Fish2 = copy(str)
  }

  case class Dog(name: String) extends Pets[Dog] {
    def rename(str: String): Dog = copy(str)
  }

  class Mammut(name: String) extends Dog(name)



/*
*                             Higher-kinded types
*
* A higher-kinded type is a type that abstracts over some type that, in turn, abstracts over another type.
* It's a way to generically abstract over entities that take type constructors.
* They allow us to write modules that can work with a wide range of objects.
*
*
*                            Ad-Hoc Polymorphism (overload)
*
* One of the easiest ways to think about ad-hoc polymorphism is in terms of a switch statement that's happening
* in the background but without a default case. In this case, the compiler switches between different code
* implementations depending on the type of input a method receives.
*
*
*
*                               Associativity
* a x (b x c) = (a x b) x c
*
*
*
*                                   Kleisli
*
* We do have:
*   f: Int => String
*   g: String => Double
* So we can compose both functions in a way: "f andThen g"
*
*   h: String => Option[Int]
*   p: Int => Option[Double]
* We can not write: "h andThen p", here is where Kleisli comes to play
*
*
*
* "flatMap" is not a hard requirement
* so basically we can implement with a map: but in this case Functor is required
*
* */

  case class Kleisli[F[_], In, Out](run: In => F[Out]) {
    import cats.FlatMap
    import cats.implicits._
    // Kleisli[F[_], In, Out] current
    def andThen[Out2](k: Kleisli[F, Out, Out2])(implicit f: FlatMap[F]): Kleisli[F, In, Out2] =
      Kleisli[F, In, Out2](in => run.apply(in).flatMap(out => k.run(out)))

    import cats.Functor
    // also we can do something like this where we put simple function: A => B
    def map[Out2](k: Out => Out2)(implicit f: Functor[F]): Kleisli[F, In, Out2] =
      Kleisli[F, In, Out2](in => run(in).map(out => k(out)))
  }



/*
*                                       Actor
*
* is a function:  Message => Behavior[Message]
*
*   Behavior[Message]
*     - can do side effect
*     - can hold a state
*     - can change the way of reacting on some messages
*     - sender() was removed from actor typed version and instead of this we should include replyTo into the msg itself
*     - has defined message protocol to talk about
* */

  case class Hello(value: String)
  case class SayHello(name: String, replyTo: ActorRef[Hello])

  object FirstActor {
    def process(): Behavior[SayHello] =
      Behaviors.receiveMessage {
        case SayHello(name, replyTo) =>
          replyTo ! Hello(name + " hello from FirstActor")
          Behaviors.same
      }
  }

  def callAnActor(): Unit = {
    implicit val timeout = Timeout(3.seconds)
    implicit val sys: ActorSystem[SayHello] = ActorSystem(FirstActor.process(), "SomeActorSystem")
    implicit val ec = sys.executionContext

    sys
      .ask(replyTo => SayHello("Jeremmy", replyTo))
      .foreach(result => println("Result is: " + result))
  }


                                            /*  Stash */

  def stash(): Behavior[StashMessage] =
    Behaviors.setup { ctx =>
      Behaviors.withStash(30) { buffer =>                     //  <-- create way
        Behaviors.receiveMessagePartial {
          case First =>
            buffer.unstashAll(unstashBehavior())              //  <-- UN-STASH MSGs
            Behaviors.same
          case s: StashMessage =>
            buffer.stash(s)                                   //  <-- STASH MSGs
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
                                          pipeTo

    Handle Future from some service and transform them into the message which are going to be send to itself

    Example:
    context.pipeToSelf(ExternalService.findPhone(name)) {
      case Success(phone) => PersonPhone(phone)
      case Failure(exception) => PhoneNotFound(name, exception)
    }


                                     SupervisorStrategy

      def apply(): Behavior[WorkerServiceCommand] = {
        Behaviors.setup[WorkerServiceCommand] {
          context =>
            val supervisedWorkerBehavior = Behaviors
              .supervise[WorkerCommands](Worker.apply())
              .onFailure[Exception](SupervisorStrategy.restart)
            val worker = context.spawn(supervisedWorkerBehavior, "worker-actor")
            handle(worker)
        }
      }




                                            Cluster


   -  Akka Cluster provides a fault-tolerant decentralized peer-to-peer based Cluster Membership Service
     with no single point of failure or single point of bottleneck.
      It does this using gossip protocols and an automatic failure detector.
     Akka Cluster allows for building distributed applications, where one application or service spans multiple nodes
     (in practice multiple ActorSystems)


    - node -> Defined by a "hostname:port:uid tuple"
    - When a new node is started it sends a message to all configured seed-nodes
    - then sends a join command to the one that answers first. If none of the seed nodes replies (might not be started yet)
      it retries this procedure until successful or shutdown.


                                            LEADER

   - There is no leader election process
   - The leader can always be recognised deterministically by any node whenever there is gossip convergence.
   - The leader is only a role, any node can be the leader and it can change between convergence rounds.
   - The leader is the first node in sorted order that is able to take the leadership role
   - The role of the leader:
      - is to "shift" members "in" and "out" of the cluster
      - changing joining members to the "up" state or exiting members to the "removed" state.
   - Currently leader actions are only triggered by receiving a new cluster state with gossip convergence.


                                           SEAD NODES

   - The seed nodes are contact points for new nodes joining the cluster.
   - When a new node is started it sends a message to all seed nodes and then sends a join command to the seed node that
     answers first.
   - The seed nodes configuration value does not have any influence on the running cluster itself, it helps them to
     find contact points to send the join command to; a new member can send this command to any current member of the
     cluster, not only to the seed nodes.

   - The actor system on a node that exited or was downed cannot join the cluster again.
     In particular, a node that was downed while being unreachable and then regains connectivity cannot rejoin the cluster.
     Instead, the process has to be restarted on the node, creating a new actor system that can go through the joining process again.

   - The seed nodes can be started in any order.
   - Node configured as the first element in the seed-nodes list must be started when initially starting a cluster.
     If it is not, the other seed-nodes will not become initialized, and no other node can join the cluster.
     The reason for the special first seed node is to avoid forming separated islands when starting from an empty cluster.
   - As soon as more than two seed nodes have been started, it is no problem to shut down the first seed node.
   - If the first seed node is restarted, it will first try to join the other seed nodes in the existing cluster.
   - Note that if you stop all seed nodes at the same time and restart them with the same seed-nodes configuration they
     will join themselves and form a new cluster, instead of joining remaining nodes of the existing cluster.
   - That is likely not desired and can be avoided by listing several nodes as seed nodes for redundancy, and don’t stop
     all of them at the same time.

   - The Configuration Compatibility Check feature ensures that all nodes in a cluster have a compatible configuration.
   - Whenever a new node is joining an existing cluster, a subset of its configuration settings (only needed) is sent
     to the nodes in the cluster for verification.
   - Once the configuration is checked on the cluster side, the cluster sends back its own set of required configuration settings.
   - The joining node will then verify if it’s compliant with the cluster configuration.
   - The joining node will only proceed if all checks pass, on both sides.
   - Akka Cluster can be used across multiple data centers, availability zones or regions, so that one Cluster can span
     multiple data centers and still be tolerant to network partitions.


                                         Singleton manager

    - Singleton actor instance among all cluster nodes or a group of nodes tagged with a specific role.
    - Started on the oldest node by creating a child actor from supplied Behavior.
    - It makes sure that at most one singleton instance is running at any point in time.
    - Always running on the oldest member with specified role.
    - When the oldest node is Leaving the cluster there is an exchange from the oldest and the new oldest before
      a new singleton is started up.
    - The cluster failure detector will notice when oldest node becomes unreachable due to things like JVM crash,
      hard shut down, or network failure. After Downing and removing that node the a new oldest node will take over
      and a new singleton actor is created.

    - To communicate with a given named singleton in the cluster you can access it though a proxy ActorRef.
    - ClusterSingleton.init for a given singletonName  ActorRef is returned.
    - If there already is a singleton manager running - is returned.

    - The proxy will route all messages to the current instance of the singleton, and keep track of the oldest node in
      the cluster and discover the singleton’s ActorRef.
    - Singleton is unavailable:
        - the proxy will buffer the messages sent to the singleton
        - deliver them when the singleton is finally available
        - if the buffer is full the proxy will drop old messages when new messages are sent via the proxy.
        - the size of the buffer is configurable and it can be disabled by using a buffer size of 0.

    Example:

    val proxy: ActorRef[Counter.Command] =
    ClusterSingleton(system)
      .init(
        SingletonActor(Behaviors.supervise(Counter())
          .onFailure[Exception](SupervisorStrategy.restart), "GlobalCounter"))




                                          Receptionist AND MessageAdaptor

  In case current actor have no idea how to handle some type of message (another protocol) but it's need it
  in this case that message type can be wrapped and messageAdaptor can handle this case.


    In case current actor does not support(have no idea how to handle) some type of message but
  we are interesting on response (and father handling) to our self and we should provide a link to our self
  we can use `ctx.messageAdaptor` which will return a link to OurSelf with a function inside
  function inside it's just a wrapper on message response

  Ability to find any actor in cluster, but first you need to register it.

  Register an actor:
    val actorRef = ctx.spawn(someBehavior(), "some-behavior")
    ctx.system.receptionist ! Register(ServiceKey[SomeMessage]("some-message"), actorRef)

  Find it:
    val adaptor = ctx.messageAdapter(WrappedListing)
    ctx.system.receptionist ! Find(SomeMessage.key, adaptor)

  Handle case in the actor when we get a response message:
    case WrappedListing(SomeMessage.key.Listing(listings)) =>
      listings.foreach(actorRef => actorRef ! M2)

  Example(full):
*/

  // message protocol declaration
  trait SomeMessage
  case object Message1 extends SomeMessage
  case class WrappedListing(listing: Listing) extends SomeMessage
  object SomeMessageProtocol {
    val key = ServiceKey[SomeMessage]("some-message")
  }

  trait SomeOtherMessage
  case object Message2 extends SomeOtherMessage
  object SomeOtherMessageProtocol {
    val key = ServiceKey[SomeOtherMessage]("some-other-message")
  }


  def initReceptionist(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      val actorRef1 = ctx.spawn(actor1Behavior(), "some-behavior")
      val actorRef2 = ctx.spawn(actor2Behavior(), "some-other-behavior")
      ctx.system.receptionist ! Register(SomeMessageProtocol.key, actorRef1)      // register an actor which handle that kind of protocol messages
      ctx.system.receptionist ! Register(SomeOtherMessageProtocol.key, actorRef2) // the same here ...

      actorRef1 ! Message1
      Behaviors.same[Nothing]
    }


  def actor1Behavior(): Behavior[SomeMessage] =
    Behaviors.receive { (ctx, msgs) =>
      msgs match {
        case Message1 =>
          ctx.system.receptionist ! Find(
            key = SomeOtherMessageProtocol.key,
            replyTo = ctx.messageAdapter(WrappedListing))
          Behaviors.same

        case WrappedListing(SomeOtherMessageProtocol.key.Listing(listings)) =>
          listings.foreach(actorRef2 => actorRef2 ! Message2)
          Behaviors.same
      }
    }

  def actor2Behavior(): Behavior[SomeOtherMessage] =
    Behaviors.receiveMessage {
      case Message2 => Behaviors.same
    }

                        /*  Sharding/Events */

  /*
  * Need to provide:
  *   - Command handler
  *   - Event handler
  *   - PersistenceId(name, id)
  *   - empty state
  *
  * Example:
  * */

  trait MySerializer
  case class CurrentBalance(amount: Int)
  case class Account(amount: Int) {
    def +(newAmount: Int): Account = Account(amount + newAmount)
    def -(newAmount: Int): Account = Account(amount - newAmount)
  }

  object Account {
    val entityTypeKey = EntityTypeKey[Account]("Account")
    val empty: Account = Account(0)
    val commandHandler: (Account, Command) => Effect[Event, Account] =
      (account, command) =>
        command match {
          case Deposit(amount) => Effect.persist(Deposited(amount))
          case Withdraw(amount) => Effect.persist(Withrawed(amount))
          case GetBalance(replyTo) => Effect.reply(replyTo)(CurrentBalance(account.amount))
        }

    val eventHandler: (Account, Event) => Account =
      (account, event) =>
        event match {
          case Deposited(amount) => account + amount
          case Withrawed(amount) => account - amount
        }
  }

  sealed trait Command extends MySerializer
  case class Deposit(amount: Int) extends Command
  case class Withdraw(amount: Int) extends Command
  case class GetBalance(replyTo: ActorRef[CurrentBalance]) extends Command

  sealed trait Event extends MySerializer
  case class Deposited(amount: Int) extends Event
  case class Withrawed(amount: Int) extends Event

  object EventHandler {
    def apply(id: Int): EventSourcedBehavior[Command, Event, Account] =
      EventSourcedBehavior.apply(
        persistenceId = PersistenceId.apply(Account.entityTypeKey.name, id.toString),
        emptyState = Account.empty,
        commandHandler = Account.commandHandler,
        eventHandler = Account.eventHandler
      )
  }



}

