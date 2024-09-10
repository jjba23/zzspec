package kafkatest

import zzspec.kafka.{Kafka, KafkaContainer, NewTopic}
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

object KafkaSpec extends ZIOSpecDefault {

  case class SomeMessage(stringValue: String, intValue: Int, stringListValue: Seq[String])

  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val logConfig = ConsoleLoggerConfig(
    LogFormat.colored,
    LogFilter.LogLevelByNameConfig.default
  )
  private val logs = Runtime.removeDefaultLoggers >>> consoleLogger(logConfig) >+> Slf4jBridge.initialize

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("Kafka tests")(basicKafkaTopicOperations, publishingToKafkaTopicWorks).provideShared(
      ZLayer.succeed(Network.SHARED),
      logs,
      ZLayer.succeed(new Slf4jLogConsumer(slf4jLogger)),
      Scope.default,
      KafkaContainer.layer,
      KafkaProducer.producerLayer
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
    } @@ TestAspect.timeout(30.seconds)

  def publishingToKafkaTopicWorks = test("""
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
    ).asJson.toString.getBytes

    val consumedMessages: Buffer[String] = Buffer.empty

    for {
      kafkaContainer <- ZIO.service[KafkaTestContainer]
      _ <- Kafka.createTopic(topic).orDie

      consumerFib <- Kafka
        .consumeAndDoWithEvents(
          groupId = "zzspec",
          bootstrapServers = Seq(kafkaContainer.getBootstrapServers()),
          topic = "test-topic2"
        )((r: ConsumerRecord[Long, Array[Byte]]) =>
          ZIO.succeed(consumedMessages += r.key.toString) *>
            Console
              .printLine(s"key: ${r.key}, value: ${r.value.toString}, consumerRecord: $r")
              .orDie
        )
        .fork

      _ <- ZIO.sleep(5.seconds)

      _ <- KafkaProducer
        .runProducer(topic.name, "1", payload)
        .orDie
    } yield assertTrue(consumedMessages.length == 1)
  } @@ TestAspect.withLiveClock @@ TestAspect.timeout(30.seconds)
}
