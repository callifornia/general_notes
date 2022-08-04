package akka.actor_typed_signals

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.SupervisorStrategy
import Worker._
import WorkerService._


///*
//* Supervise strategy inside the worker itself
//* */
//object Worker {
//  def apply(): Behavior[WorkerCommands] =
//    Behaviors.supervise {
//      Behaviors.receive[WorkerCommands] {
//        (context, message) => message match
//          case WashDish =>
//            context.log.info("Worker ... WashDish")
//            Behaviors.same
//          case ThrowAnException =>
//            context.log.info(s"Worker ... ThrowAnException")
//            throw Exception("Custom Exception was thrown...")
//          case Restart =>
//            context.log.info(s"Worker ... Restart")
//            Behaviors.unhandled
//          case Stop =>
//            context.log.info(s"Worker ... Stop")
//            Behaviors.stopped
//          case CleanKitchen =>
//            context.log.info(s"Worker ... CleanKitchen")
//            Behaviors.same
//      }.receiveSignal {
//        case (ctx, signal) =>
//          ctx.log.info("Worker SIGNAL HANDLER. Signal: {} ", signal)
//          Behaviors.same
//      }
//    }.onFailure[Exception](SupervisorStrategy.restart)
//
//
//  sealed trait WorkerCommands
//  case object WashDish extends WorkerCommands
//  case object CleanKitchen extends WorkerCommands
//
//  case object ThrowAnException extends WorkerCommands
//  case object Restart extends WorkerCommands
//  case object Stop extends WorkerCommands
//}

/*
* Supervise strategy outside of worker actor
* */
object Worker {
  def apply(): Behavior[WorkerCommands] =
    Behaviors.receive[WorkerCommands] {
      (context, message) => message match {
        case WashDish =>
          context.log.info("Worker ... WashDish")
          Behaviors.same
        case ThrowAnException =>
          context.log.info(s"Worker ... ThrowAnException")
          throw new Exception("Custom Exception was thrown...")
        case Restart =>
          context.log.info(s"Worker ... Restart")
          Behaviors.unhandled
        case Stop =>
          context.log.info(s"Worker ... Stop")
          Behaviors.stopped
        case CleanKitchen =>
          context.log.info(s"Worker ... CleanKitchen")
          Behaviors.same
      }
    }.receiveSignal {
      case (ctx, signal) =>
        ctx.log.info("Worker SIGNAL HANDLER. Signal: {} ", signal)
        Behaviors.same
    }


  sealed trait WorkerCommands
  case object WashDish extends WorkerCommands
  case object CleanKitchen extends WorkerCommands

  case object ThrowAnException extends WorkerCommands
  case object Restart extends WorkerCommands
  case object Stop extends WorkerCommands
}


object WorkerService {
  // Supervisor strategy on WorkerService level, not inside worker
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

//  def apply(): Behavior[WorkerServiceCommand] = {
//    Behaviors.setup[WorkerServiceCommand] {
//      context =>
//        val worker = context.spawn(Worker(), "worker-actor")
//        context.watch(worker)
//        handle(worker)
//    }
//  }


  def handle(worker: ActorRef[WorkerCommands]): Behavior[WorkerServiceCommand] = {
    Behaviors.receive[WorkerServiceCommand] { (context, message) =>
      message match {
        case Clean =>
          context.log.info(s"WorkerService ... going to send WashDish, CleanKitchen")
          worker ! Worker.WashDish
          worker ! Worker.CleanKitchen
          handle(worker)
        case DoSomethingStrange =>
          context.log.info(s"WorkerService ... going to send ThrowAnException")
          worker ! Worker.ThrowAnException
          handle(worker)
        case Stop =>
          context.log.info(s"WorkerService ... going to send Stop")
          worker ! Worker.Stop
          handle(worker)
        case Restart =>
          context.log.info(s"WorkerService ... going to send Restart")
          worker ! Worker.Restart
          handle(worker)
      }
    }.receiveSignal {
      case (ctx, signal) =>
        ctx.log.info("WorkerService received signal from a man ....{}", signal)
        handle(worker)
    }
  }

  sealed trait WorkerServiceCommand
  case object Clean extends WorkerServiceCommand
  case object DoSomethingStrange extends WorkerServiceCommand
  case object Stop extends WorkerServiceCommand
  case object Restart extends WorkerServiceCommand
}


object Main {
  def main(args: Array[String]): Unit = {
    val workerService = ActorSystem(WorkerService.apply(), "worker-service")
    workerService ! Clean
    workerService ! WorkerService.DoSomethingStrange

    workerService ! Clean
    Thread.sleep(20000)
  }
}
