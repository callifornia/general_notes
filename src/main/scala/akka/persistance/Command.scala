package akka.persistance

import akka.actor.typed.ActorRef

sealed trait Command extends MySerializer
case class Deposit(amount: Int) extends Command
case class Withdraw(amount: Int) extends Command
case class GetBalance(replyTo: ActorRef[CurrentBalance]) extends Command
case class CurrentBalance(amount: Int)

sealed trait Event extends MySerializer
case class Deposited(amount: Int) extends Event
case class Withrawed(amount: Int) extends Event

