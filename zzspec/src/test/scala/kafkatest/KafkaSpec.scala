package kafkatest

import zzspec.kafka.{Kafka, KafkaContainer, NewTopic}
import org.apache.kafka.common.config.TopicConfig
import org.testcontainers.containers
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.{KafkaContainer => KafkaTestContainer, Network}
import zio._
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.test._

object KafkaSpec extends ZIOSpecDefault {

  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val logConfig = ConsoleLoggerConfig(
    LogFormat.colored,
    LogFilter.LogLevelByNameConfig.default
  )
  private val logs = Runtime.removeDefaultLoggers >>> consoleLogger(logConfig) >+> Slf4jBridge.initialize

  def spec: Spec[Environment & TestEnvironment & Scope, Any] =
    suite("Kafka tests")(basicKafkaTopicOperations).provideShared(
      ZLayer.succeed(Network.SHARED),
      logs,
      ZLayer.succeed(new Slf4jLogConsumer(slf4jLogger)),
      KafkaContainer.layer,
    )

  def basicKafkaTopicOperations: Spec[KafkaTestContainer, Nothing] =
    test("""
      Creating and deleting topics works
    """) {
      val topic = NewTopic(
        name = "test-topic",
        partitions = 2,
        replicationFactor = 2,
        configs = Map(
          TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT,
        ),
      )
      for {
        _ <- Kafka.createTopic(topic).orDie
        _ <- Kafka.deleteTopic(topic.name).orDie
      } yield assertTrue(1 == 1)
    } @@ TestAspect.timeout(1.minute)

  def publishingToKafkaTopicWorks: Spec[containers.KafkaContainer, Nothing] = test("""
    TODO: Publishing messages to a Kafka topic works as expected
  """) {
    val topic = NewTopic(
      name = "test-topic2",
      partitions = 2,
      replicationFactor = 2,
      configs = Map(
        TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT,
      ),
    )

    for {
      _ <- Kafka.createTopic(topic).orDie
      // _ <- KafkaProducer
      //        .runProducer[SomeMessage](topic.name, "1", message)
      //        .orDie
    } yield assertTrue(1 == 1)
  } @@ TestAspect.timeout(1.minute)
}
