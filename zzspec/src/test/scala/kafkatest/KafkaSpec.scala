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
import zio.logging._
import zzspec.ZZSpec._
import zzspec.ZZContract._

object KafkaSpec extends ZIOSpecDefault {

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("Kafka tests")(basicKafkaTopicOperations, publishingAndConsumingKafkaTopicWorks)
      .provideShared(
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
    """.strip) {
      for {
        topic <- newTopic
        _     <- Kafka.createTopic(topic).orDie
        _     <- Kafka.deleteTopic(topic.name).orDie
      } yield assertTrue(1 == 1)
    } @@ TestAspect.timeout(8.seconds)

  case class SomeMessage(stringValue: String, intValue: Int, stringListValue: Seq[String])

  def publishingAndConsumingKafkaTopicWorks = {
    val testCaseName = """
      Publishing and consuming simple messages to a Kafka topic works as expected
    """.strip

    test(testCaseName) {
      for {
        // given
        topic                <- newTopic
        _                    <- Kafka.createTopic(topic)
        _                    <- KafkaProducer.produce(
                                  topicName = topic.name,
                                  key = "1",
                                  value = SomeMessage(
                                    stringValue = "stringValue",
                                    intValue = 999,
                                    stringListValue = Seq("a", "b", "c")
                                  ).asJson.toString()
                                )

        // when
        consumedMessages     <-
          ZIO.serviceWithZIO[Consumer](
            _.plainStream(Subscription.Topics(Set(topic.name)), Serde.string, Serde.string)
              .take(1)
              .runCollect
          )

        // then
        firstMessage         <- parseJson(consumedMessages.map(r => (r.record.key, r.record.value)).head._2)
        expectedFirstMessage <- contractFromTestName(
                                  name = testCaseName,
                                  modulePath = "zzspec",
                                  orElse = Some(firstMessage.toPrettyString())
                                ).flatMap(parseJson)

        _ <- ZIO.foreach(consumedMessages)(m => ZIO.logInfo(m.toString()))
        _ <- ZIO.logInfo(s"expectedFirstMessage: $expectedFirstMessage")

        _ <- Kafka.deleteTopic(topic.name)

        expectedLogsArePresent <- checkLogs(Set(_.contains("key = 1")))
      } yield assertTrue(
        consumedMessages.length == 1,
        firstMessage == expectedFirstMessage,
        expectedLogsArePresent
      )
    } @@
    TestAspect.withLiveClock @@
    TestAspect.timeout(8.seconds)
  }
}
