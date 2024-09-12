package zzspec.opensearch

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.searches.SearchRequest
import zio.{Scope, Task, ZIO, ZLayer}
import zzspec.opensearch.OpensearchContainer._

object Opensearch {

  import ZIOTaskImplicits._

  private type SearchEff[T] = ZIO[Scope with Client, Throwable, T]
  val layer: ZLayer[Container, Throwable, Client] =
    ZLayer.scoped {
      for {
        opensearchContainer <- ZIO.service[Container]
        opensearchUrl        =
          new StringBuilder("http://")
            .append(opensearchContainer.value.getHost)
            .append(":")
            .append(
              opensearchContainer.value.getMappedPort(9200).toString,
            )
            .toString()
        client               = ElasticClient(ElasticProperties(opensearchUrl))
      } yield Client(client)
    }

  def countDocuments(indexName: String): SearchEff[Long] = for {
    client         <- ZIO.service[Client]
    searchResponse <-
      client.value.execute(search(indexName).restTotalHitsAsInt(true))
  } yield searchResponse.result.totalHits

  def createNewIndex(indexName: String): SearchEff[Unit] =
    ZIO.serviceWithZIO[Client](_.value.execute(createIndex(indexName))).unit

  def deleteAnIndex(indexName: String): SearchEff[Unit] =
    ZIO.serviceWithZIO[Client](_.value.execute(deleteIndex(indexName))).unit

  def indexDocument[T: Indexable](
    indexName: String,
    document: T
  ): SearchEff[Unit] =
    ZIO
      .serviceWithZIO[Client](
        _.value.execute(
          indexInto(IndexAndType(index = indexName, `type` = "_doc"))
            .doc(document)
            .refresh(RefreshPolicy.Immediate),
        ),
      )
      .unit

  def searchDocument[T: HitReader](
    searchRequest: SearchRequest
  ): SearchEff[List[Either[Throwable, T]]] =
    for {
      client         <- ZIO.service[Client]
      resp           <- client.value.execute(searchRequest.restTotalHitsAsInt(true))
      maybeParsedDocs = resp.result.safeTo[T].toList.map(_.toEither)
    } yield maybeParsedDocs

  case class Client(value: ElasticClient)
}

// TODO: Clean up when upgrading Elastic4s to version 7.3.4 or higher
// copied from https://github.com/sksamuel/elastic4s/blob/master/elastic4s-effect-zio/src/main/scala/com/sksamuel/elastic4s/zio/instances/TaskInstances.scala
trait ZIOTaskImplicits extends {

  implicit val functor: http.Functor[Task] = new http.Functor[Task] {
    override def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)
  }

  implicit val executor: http.Executor[Task] =
    (client: http.HttpClient, request: http.ElasticRequest) =>
      ZIO.asyncZIO { cb =>
        ZIO.attempt(client.send(request, v => cb(ZIO.fromEither(v))))
      }
}

object ZIOTaskImplicits extends ZIOTaskImplicits
