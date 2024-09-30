package kafkatest

import io.circe.generic.auto._
import io.circe.syntax._
import zio._
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.test._
import io.github.jjba23.zzspec.ZZContract._
import io.github.jjba23.zzspec.ZZSpec._
import io.github.jjba23.zzspec.kafka.Kafka._
import io.github.jjba23.zzspec.kafka._

object KafkaSpec extends ZIOSpecDefault {

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("Kafka tests")(
      basicKafkaTopicOperations,
      publishingAndConsumingKafkaTopicWorks,
      publishingAndConsuming6KafkaTopicWorks
    )
      .provideShared(
        containerLogger(),
        networkLayer,
        Scope.default,
        KafkaContainer.layer(),
        KafkaProducer.layer,
        KafkaConsumer.layer()
      )

  def basicKafkaTopicOperations =
    test("""
      Creating and deleting topics works
    """.strip) {
      for {
        topic <- newTopic
        _     <- createTopic(topic).orDie
        _     <- deleteTopic(topic.name).orDie
      } yield assertTrue(1 == 1)
    } @@ TestAspect.timeout(8.seconds)

  case class SomeMessage(
    stringValue: String,
    intValue: Int,
    stringListValue: Seq[String]
  )

  def publishingAndConsumingKafkaTopicWorks = {
    val testCaseName = """
      Publishing and consuming simple messages to a Kafka topic works as expected
    """.strip

    test(testCaseName) {
      for {
        // given
        topic                <- newTopic
        _                    <- createTopic(topic)
        someUUID             <- nextRandom
        _                    <- produce(topicName = topic.name)(
                                  key = someUUID.toString,
                                  value = SomeMessage(
                                    stringValue = "stringValue",
                                    intValue = 999,
                                    stringListValue = Seq("a", "b", "c")
                                  ).asJson.toString()
                                )

        // when
        consumedMessages     <-
          ZIO.serviceWithZIO[Consumer](
            _.plainStream(
              Subscription.Topics(Set(topic.name)),
              Serde.string,
              Serde.string
            )
              .take(1)
              .runCollect
          )

        // then
        firstMessage         <-
          parseJson(
            consumedMessages.map(r => (r.record.key, r.record.value)).head._2
          )
        expectedFirstMessage <- contractFromTestName(
                                  name = testCaseName,
                                  modulePath = "zzspec",
                                  orElse = Some(firstMessage.toPrettyString())
                                ).flatMap(parseJson)

        _ <- ZIO.foreach(consumedMessages)(m => ZIO.logInfo(m.toString()))
        _ <- ZIO.logInfo(s"expectedFirstMessage: $expectedFirstMessage")

        expectedLogsArePresent <- checkLogs(Set(_.contains(s"key = $someUUID")))

        // cleanup
        _ <- deleteTopic(topic.name)
      } yield assertTrue(
        consumedMessages.length == 1,
        firstMessage == expectedFirstMessage,
        expectedLogsArePresent
      )
    } @@
    TestAspect.withLiveClock @@
    TestAspect.timeout(8.seconds)
  }

  def publishingAndConsuming6KafkaTopicWorks = {
    val testCaseName = """
      Publishing and consuming 6 simple messages to a Kafka topic works as expected
    """.strip

    test(testCaseName) {
      for {
        // given
        topic            <- newTopic
        _                <- createTopic(topic)
        _                <- ZIO.foreach(1 to 6)(i => {
                              for {
                                someUUID <- nextRandom
                                _        <- produce(topicName = topic.name)(
                                              key = someUUID.toString,
                                              value = SomeMessage(
                                                stringValue = i.toString,
                                                intValue = i,
                                                stringListValue = Seq("a", "b", "c")
                                              ).asJson.toString()
                                            )
                              } yield ()
                            })

        // when
        consumedMessages <-
          ZIO.serviceWithZIO[Consumer](
            _.plainStream(
              Subscription.Topics(Set(topic.name)),
              Serde.string,
              Serde.string
            )
              .take(6)
              .runCollect
          )

        // then
        parsedMessages   <-
          ZIO.foreach(
            consumedMessages.map(r => (r.record.key, r.record.value)).map(_._2)
          )(parseJson)
        consumedInts      = parsedMessages.map(rec => rec.get("intValue").asInt())

        // cleanup
        _ <- deleteTopic(topic.name)
      } yield assertTrue(
        consumedMessages.length == 6,
        consumedInts.toList == (1 to 6).toList
      )
    } @@
    TestAspect.withLiveClock @@
    TestAspect.timeout(8.seconds)
  }
}
