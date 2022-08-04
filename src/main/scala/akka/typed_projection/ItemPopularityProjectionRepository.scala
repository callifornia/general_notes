package akka_typed_projection

import akka.Done
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

trait ItemPopularityProjectionRepository {
  def update(itemId: String, delta: Int): Future[Done]
  def getItem(itemId: String): Future[Option[Long]]
}

object ItemPopularityProjectionRepositoryImpl {
  val keyspace = "akka_projection"
  val popularityTable = "item_popularity"
}

class ItemPopularityProjectionRepositoryImpl(session: CassandraSession)(implicit val ec: ExecutionContext)
  extends ItemPopularityProjectionRepository {
  import ItemPopularityProjectionRepositoryImpl._

  override def update(itemId: String, delta: Int): Future[Done] = {
    session.executeWrite(
      s"UPDATE $keyspace.$popularityTable SET count = count + ? WHERE item_id = ?",
      java.lang.Long.valueOf(delta),
      itemId)
  }

  override def getItem(itemId: String): Future[Option[Long]] = {
    session
      .selectOne(s"SELECT item_id, count FROM $keyspace.$popularityTable WHERE item_id = ?", itemId)
      .map(opt => opt.map(row => row.getLong("count").longValue()))
  }
}