package zzspec.kafka

import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import zio._
import zio.kafka.producer.{Producer, ProducerSettings}

/** KafkaProducer exports a layer of a KafkaProducer to assist in the publishing
  * of messages to Kafka topics in the testcontainer.
  */
object KafkaProducer {

  val layer: ZLayer[KafkaTestContainer with Scope, Throwable, Producer] =
    ZLayer(
      ZIO.serviceWithZIO[KafkaTestContainer](kafkaContainer =>
        Producer.make(
          settings = ProducerSettings(
            List(
              s"${kafkaContainer.getHost}:${kafkaContainer.getMappedPort(9092)}"
            )
          )
        )
      )
    )
}
