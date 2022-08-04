package akka.persistance

import akka.actor.TimerSchedulerImpl.Timer
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.util.Timeout

import scala.concurrent.duration.DurationInt


object Main {

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


  object EventHandler {
    def apply(id: Int): EventSourcedBehavior[Command, Event, Account] =
      EventSourcedBehavior.apply(
        persistenceId = PersistenceId.apply(Account.entityTypeKey.name, id.toString),
        emptyState = Account.empty,
        commandHandler = Account.commandHandler,
        eventHandler = Account.eventHandler
      )
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(EventHandler.apply(1), "CustomActorSystem")
    implicit val timer = Timeout(3.seconds)
    implicit val ec = system.executionContext

    system ! Deposit(777)
    system ! Withdraw(100)
    system
      .ask(replyTo => GetBalance(replyTo))
      .foreach(currentBalance => println("Current balance is: " + currentBalance))
    Thread.sleep(2000)
  }
}
