package kafkatest

import zzspec.kafka._
import org.apache.kafka.common.config.TopicConfig
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import org.testcontainers.containers.Network
import zio._
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.test._
import zzspec.kafka.KafkaProducer
import io.circe.generic.auto._, io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.collection.mutable.Buffer
import zio.kafka.consumer.Consumer
import zio.kafka.consumer.Subscription
import zio.kafka.serde.Serde
import zio.kafka.testkit
import zio.kafka.consumer.ConsumerSettings
import com.fasterxml.jackson.databind.ObjectMapper

object KafkaSpec extends ZIOSpecDefault {

  case class SomeMessage(stringValue: String, intValue: Int, stringListValue: Seq[String])

  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val logConfig = ConsoleLoggerConfig(
    LogFormat.colored,
    LogFilter.LogLevelByNameConfig.default
  )
  private val logs = Runtime.removeDefaultLoggers >>> consoleLogger(logConfig) >+> Slf4jBridge.initialize
  private val mapper = new ObjectMapper()

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("Kafka tests")(basicKafkaTopicOperations, publishingAndConsumingKafkaTopicWorks).provideShared(
      ZLayer.succeed(Network.SHARED),
      logs,
      ZLayer.succeed(new Slf4jLogConsumer(slf4jLogger)),
      Scope.default,
      KafkaContainer.layer,
      KafkaProducer.producerLayer,
      Kafka.consumerLayer
    )

  def basicKafkaTopicOperations =
    test("""
      Creating and deleting topics works
    """) {
      val topic = NewTopic(
        name = "test-topic",
        partitions = 1,
        replicationFactor = 1,
        configs = Map(
          TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT,
        ),
      )
      for {
        _ <- Kafka.createTopic(topic).orDie
        _ <- Kafka.deleteTopic(topic.name).orDie
      } yield assertTrue(1 == 1)
    } @@ TestAspect.timeout(10.seconds)

  def publishingAndConsumingKafkaTopicWorks = test("""
    Publishing and consuming simple messages to a Kafka topic works as expected
  """) {
    val topic = NewTopic(
      name = "test-topic2",
      partitions = 1,
      replicationFactor = 1,
      configs = Map(
        TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT,
      ),
    )

    val payload = SomeMessage(
      stringValue = "stringValue",
      intValue = 999,
      stringListValue = Seq("a", "b", "c")
    ).asJson.toString

    for {
      kafkaContainer <- ZIO.service[KafkaTestContainer]

      _ <- Kafka.createTopic(topic).orDie

      _ <- KafkaProducer
        .runProducer(topic.name, "1", payload)
        .orDie
      _ <- ZIO.logInfo(s"!!! Produced message: $payload")
      consumer <- ZIO.service[Consumer]
      records <- consumer
        .plainStream(Subscription.Topics(Set(topic.name)), Serde.string, Serde.string)
        .take(1)
        .runCollect

      consumedMessages = records.map(r => (r.record.key, r.record.value))

      expectedFirstMessage =
        """ { "stringValue": "stringValue", "intValue": 999, "stringListValue": ["a", "b", "c"]}  """
    } yield assertTrue(
      consumedMessages.length == 1 && consumedMessages.headOption.map(m => mapper.readTree(m._2)) == Some(
        mapper.readTree(expectedFirstMessage)
      )
    )
  } @@ TestAspect.withLiveClock @@ TestAspect.timeout(10.seconds)
}
