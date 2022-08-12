package akka.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.HttpApp

import scala.io.StdIn
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    println("heasdasdllo world")
    MinimalHttpServer.runServer()
  }
}

object ServerRoutes {
  val routes = {
    pathPrefix("v1") {
      path("id" / Segment) {id =>
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
            s"<h1>Hello $id from Akka Http!</h1>"))
        } ~
          post {
            entity(as[String]) {entity =>
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
                s"<b>Thanks $id for posting your message <i>$entity</i> </ b > "))
            }
          }
      }
    }
    //    pathPrefix("v1") {
    //      path("id") { id =>
    //        get {
    //          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
    //            s"<h1>Hello world</h1>"))
    //        } ~
    //          post {
    //            entity(as[String]) { entity =>
    //              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
    //                s"Thank for posting your message: $entity"))
    //            }
    //          }
    //      }
    //    }
  }
}


object MinimalHttpServer {
  implicit val system = ActorSystem(Behaviors.empty, "something")
  implicit val ec = system.executionContext


  def runServer() = {
    val binded = Http().newServerAt("localhost", 8080).bind(ServerRoutes.routes)
    println("Server started ...")
    StdIn.readLine()
    binded.flatMap(_.unbind()).onComplete {
      case Success(v) => println("Server shut down successfully"); system.terminate()
      case Failure(ex) => println("Server shut down with an error: " + ex); system.terminate()
    }
  }
}