package opensearchtest

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import io.circe._
import io.circe.generic.semiauto.deriveCodec
import io.circe.parser._
import io.circe.syntax._
import zzspec.opensearch.Opensearch._
import zzspec.opensearch.{Opensearch, OpensearchContainer}
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.test._

import java.util.UUID
import scala.util.Try

case class DummyData(someString: String)

object DummyData {
  implicit val codec: Codec[DummyData] = deriveCodec[DummyData]
}

object OpensearchSpec extends ZIOSpecDefault {

  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val logConfig = ConsoleLoggerConfig(
    LogFormat.colored,
    LogFilter.LogLevelByNameConfig.default
  )
  private val logs = Runtime.removeDefaultLoggers >>> consoleLogger(logConfig) >+> Slf4jBridge.initialize
  private implicit object IndexableDummyData extends Indexable[DummyData] {
    def json(t: DummyData): String = t.asJson.toString()
  }
  private implicit object HitReaderDummyData extends HitReader[DummyData] {
    def read(hit: Hit): Try[DummyData] = decode[DummyData](hit.sourceAsString).toTry
  }

  def spec: Spec[Environment & TestEnvironment & Scope, Any] =
    suite("Opensearch query tests")(basicOpensearchOperations).provideShared(
      Scope.default,
      ZLayer.succeed(Network.SHARED),
      logs,
      ZLayer.succeed(new Slf4jLogConsumer(slf4jLogger)),
      OpensearchContainer.layer,
      OpensearchContainer.Settings.default,
      Opensearch.layer,
    )

  def basicOpensearchOperations: Spec[Scope & Opensearch.Client & OpensearchContainer.Container & Scope, Throwable] =
    test("""
    Delete an index.
    Create an index.
    Verify amount of documents is 0.
    Insert 2 documents.
    Verify 2 documents are present.
    Verify querying for a string meets expectation.
    Verify querying for an int meets expectation.
    Verify querying for a boolean meets expectation.
    Verify fetching and parsing a document meets expectation.
    """) {

      val testIndex = s"index-${UUID.randomUUID()}"
      val someDocument = DummyData("some document")
      val someOtherDocument = DummyData("open search")

      for {
        _ <- deleteAnIndex(testIndex)
        _ <- createNewIndex(testIndex)
        initialDocumentCountInTestIndex <- countDocuments(testIndex)
        _ <- indexDocument(testIndex, someDocument)
        _ <- indexDocument(testIndex, someOtherDocument)
        afterInsertDocumentCountInTestIndex <- countDocuments(testIndex)

        constrainedResultSet <- searchDocument[DummyData](
          search(testIndex).query(boolQuery().must(matchQuery("someString", "open search"))),
        )
        parsedDocs = constrainedResultSet.collect { case Right(value) => value }
        errorDocs = constrainedResultSet.collect { case Left(value) => value }

      } yield assertTrue(
        initialDocumentCountInTestIndex == 0,
        afterInsertDocumentCountInTestIndex == 2,
        constrainedResultSet.size == 1,
        parsedDocs.size == 1,
        errorDocs.isEmpty,
        parsedDocs.head.someString == "open search",
      )
    }
}
