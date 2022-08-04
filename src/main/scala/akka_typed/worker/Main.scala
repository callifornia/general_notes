package akka_typed.worker

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import Guardian.Start

object Main {
    def main(args: Array[String]): Unit = {
      val system = ActorSystem(Guardian.apply(), "Guardian")
      system ! Start("11111111111" :: "222222222222" :: Nil)
    }
}


object Guardian {
  sealed trait Command
  case class Start(tasks: List[String]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val manager = context.spawn(Manager(), "manager")
      Behaviors.receiveMessage {
        case Start(tasks) =>
          manager ! Manager.Delegate(tasks)
          Behaviors.same
      }
    }
}


object Manager {
  sealed trait Command
  case class Delegate(task: List[String]) extends Command
  case class WorkerAdapter(response: Worker.Response) extends Command


  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val workerAdapter = context.messageAdapter(response => WorkerAdapter(response))
      Behaviors.receiveMessage {
        case Delegate(tasks) =>
          context.log.info("Manager received some works: " + tasks)
          tasks.foreach { task =>
            context.log.info("Going to spawn the actor for this work: " + task)
            val worker = context.spawn(Worker.apply(), "worker-" + task)
            worker ! Worker.Do(workerAdapter, task)
          }
          Behaviors.same[Command]
        case WorkerAdapter(Worker.Done(summaries)) =>
          context.log.info("Manager notified about worker done on task: " + summaries)
          Behaviors.same[Command]
      }
    }
}


object Worker {
  sealed trait Command
  case class Do(replyTo: ActorRef[Response], task: String) extends Command

  sealed trait Response
  case class Done(summaries: String) extends Response

  def apply(): Behavior[Command] = {
    Behaviors.receive {
      case (context, Do(replyTo, task)) =>
        context.log.info("Worker: starting on task: " + task)
        replyTo ! Done("Worker done: " + task)
        Behaviors.same[Command]
    }
  }
}






