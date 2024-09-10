package kafkatest

import zzspec.kafka._
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import zio._
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

import zzspec.ZZSpec._
import zzspec.ZZContract

object KafkaSpec extends ZIOSpecDefault {

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("Kafka tests")(basicKafkaTopicOperations, publishingAndConsumingKafkaTopicWorks()).provideShared(
      containerLogger,
      networkLayer,
      Scope.default,
      KafkaContainer.layer,
      KafkaProducer.layer,
      KafkaConsumer.layer
    )

  def basicKafkaTopicOperations =
    test("""
      Creating and deleting topics works
    """) {
      val topic = newTopic()
      for {
        _ <- Kafka.createTopic(topic).orDie
        _ <- Kafka.deleteTopic(topic.name).orDie
      } yield assertTrue(1 == 1)
    } @@ TestAspect.timeout(8.seconds)

  def publishingAndConsumingKafkaTopicWorks(testCaseName: String = """
    Publishing and consuming simple messages to a Kafka topic works as expected
  """) = test(testCaseName) {

    case class SomeMessage(stringValue: String, intValue: Int, stringListValue: Seq[String])

    val topic = newTopic()
    for {
      // given
      kafkaContainer <- ZIO.service[KafkaTestContainer]
      _ <- Kafka.createTopic(topic).orDie

      // given - messages
      _ <- KafkaProducer
        .runProducer(
          topic.name,
          "1",
          SomeMessage(
            stringValue = "stringValue",
            intValue = 999,
            stringListValue = Seq("a", "b", "c")
          ).asJson.toString
        )
        .orDie

      // when
      consumer <- ZIO.service[Consumer]
      records <- consumer
        .plainStream(Subscription.Topics(Set(topic.name)), Serde.string, Serde.string)
        .take(1)
        .runCollect

      consumedMessages = records.map(r => (r.record.key, r.record.value))
      firstMessage <- parseJson(consumedMessages.head._2).orDie

      // then
      expectedFirstMessage <- ZZContract
        .fromTestName(
          name = testCaseName,
          className = this.getClass().toString(),
          extension = ".json",
          orElse = Some(firstMessage.toPrettyString())
        )
        .flatMap(parseJson)
        .orDie

      _ <- ZIO.logInfo(s"expectedFirstMessage: $expectedFirstMessage")
      _ <- ZIO.logInfo(s"consumedMessages: $consumedMessages")

    } yield assertTrue(
      consumedMessages.length == 1 && firstMessage == expectedFirstMessage
    )
  } @@ TestAspect.withLiveClock @@ TestAspect.timeout(8.seconds)
}
