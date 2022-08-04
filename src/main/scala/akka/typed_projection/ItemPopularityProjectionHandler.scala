package akka_typed_projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.LoggerOps
import scala.concurrent.{ExecutionContext, Future}

object ItemPopularityProjectionHandler {
  val logInterval = 10
}

class ItemPopularityProjectionHandler(tag: String,
                                      system: ActorSystem[_],
                                      repo: ItemPopularityProjectionRepository)

  extends Handler[EventEnvelope[ShoppingCartEvent]]() {



  private var logCounter: Int = 0
  private val log = LoggerFactory.getLogger(getClass)
  private implicit val ec: ExecutionContext = system.executionContext


  /**
   * The Envelope handler to process events.
   */
  override def process(envelope: EventEnvelope[ShoppingCartEvent]): Future[Done] =
    (envelope.event match {
      case ItemAdded(_, itemId, quantity)                            => repo.update(itemId, quantity)
      case ItemQuantityAdjusted(_, itemId, newQuantity, oldQuantity) => repo.update(itemId, newQuantity - oldQuantity)
      case ItemRemoved(_, itemId, oldQuantity)                       => repo.update(itemId, 0 - oldQuantity)
      case _: CheckedOut                                             => Future.successful(Done) // skip
    })
      .map{result => logItemCount(envelope.event); result}



  /**
   * Log the popularity of the item in every `ItemEvent` every `LogInterval`.
   */
  private def logItemCount(event: ShoppingCartEvent): Unit = event match {
    case itemEvent: ItemEvent =>
      logCounter += 1
      if (logCounter == ItemPopularityProjectionHandler.logInterval) {
        logCounter = 0
        val itemId = itemEvent.itemId
        repo.getItem(itemId).foreach {
          case Some(count) =>
            log.infoN("ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]", tag, itemId, count)
          case None =>
            log.info2("ItemPopularityProjectionHandler({}) item popularity for '{}': [0]", tag, itemId)
        }
      }
    case _ => ()
  }
}
