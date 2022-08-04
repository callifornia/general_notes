package akka_typed_projection

import java.time.Instant


/* Events */
sealed trait ShoppingCartEvent extends MySerializer {
  def cartId: String
}

sealed trait ItemEvent extends ShoppingCartEvent {
  def itemId: String
}

final case class ItemAdded(cartId: String,
                           itemId: String,
                           quantity: Int) extends ItemEvent

final case class ItemRemoved(cartId: String,
                             itemId: String,
                             oldQuantity: Int) extends ItemEvent

final case class ItemQuantityAdjusted(cartId: String,
                                      itemId: String,
                                      newQuantity: Int,
                                      oldQuantity: Int) extends ItemEvent

final case class CheckedOut(cartId: String,
                            eventTime: Instant) extends ShoppingCartEvent


object ShoppingCartTags {
  val single = "shopping-cart"
  val tags = Vector("carts-0", "carts-1", "carts-2")
}