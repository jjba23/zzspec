package io.github.jjba23.zzspec.kafka

import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import zio._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer._

/** KafkaConsumer exports a layer of a KafkaConsumer to assist in the
  * consumption of messages from Kafka topics in the testcontainer.
  */
object KafkaConsumer {

  /** provides a ZLayer of a KafkaConsumer bound to the testcontainer
    *
    * @param groupId
    *   is the consumer groupId
    * @param pollTimeout
    *   is the max duration a poll for new messages should last
    * @param offsetRetrievalStrategy
    *   is the strategy for dealing with offsets, for consuming messages from
    *   the oldest to newest, use [AutoOffsetStrategy.Earliest]
    */
  def layer(
    groupId: String = "zzspec",
    pollTimeout: Duration = 100.millis,
    offsetRetrievalStrategy: OffsetRetrieval =
      OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest)
  ): ZLayer[KafkaContainer with Scope, Throwable, Consumer] =
    ZLayer.scoped(
      ZIO.serviceWithZIO[KafkaTestContainer](kafkaContainer =>
        Consumer.make(
          ConsumerSettings(
            bootstrapServers = List(kafkaContainer.getBootstrapServers())
          ).withGroupId(groupId)
            .withOffsetRetrieval(offsetRetrievalStrategy)
            .withPollTimeout(pollTimeout)
        )
      )
    )
}
