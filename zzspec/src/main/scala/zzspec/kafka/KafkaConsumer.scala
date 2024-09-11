package zzspec.kafka

import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import zio._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer._

object KafkaConsumer {

  def layer: ZLayer[KafkaContainer with Scope, Throwable, Consumer] = ZLayer.fromZIO {
    for {
      kafkaContainer <- ZIO.service[KafkaTestContainer]
      consumer       <- Consumer.make(
                          ConsumerSettings(
                            bootstrapServers = List(kafkaContainer.getBootstrapServers())
                          ).withGroupId("zzspec")
                            .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
                            .withPollTimeout(100.millis)
                        )
    } yield consumer
  }
}
