package akka_typed_projection
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.projection.eventsourced.EventEnvelope
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import com.typesafe.config.ConfigFactory


/*
* https://doc.akka.io/docs/akka-projection/current/getting-started/index.html
* https://github.com/akka/akka-projection/blob/master/docs/src/main/paradox/getting-started/index.md
*
*
*
* */
object Main {
  def main(args: Array[String]): Unit = {
    shoppingCart()
    Thread.sleep(10000)
  }


  def shoppingCart(): Unit = {
    ActorSystem(
      Behaviors.setup[String] { context =>
        val system = context.system
        val provider = sourceProvider(system)
        val projection = cassandraProjection(system, provider)

        context.spawn(ProjectionBehavior.apply(projection), projection.projectionId.id)
        Behaviors.empty
      },
      "ShoppingCartApp",
      ConfigFactory.load("guide-shopping-cart-app.conf"))
  }


  def cassandraProjection[T](system: ActorSystem[T],
                             provider: SourceProvider[Offset, EventEnvelope[ShoppingCartEvent]]) =
    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId.apply("shopping-carts", ShoppingCartTags.single),
      sourceProvider = provider,
      handler = () =>
        new ItemPopularityProjectionHandler(
          ShoppingCartTags.single,
          system,
          new ItemPopularityProjectionRepositoryImpl(
            CassandraSessionRegistry(system).sessionFor("akka.projection.cassandra.session-config"))(system.executionContext)))



  def sourceProvider[T](system: ActorSystem[T]): SourceProvider[Offset, EventEnvelope[ShoppingCartEvent]] =
    EventSourcedProvider
      .eventsByTag[ShoppingCartEvent](
        system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = ShoppingCartTags.single)
}
