package zzspec.kafka

import org.apache.kafka.clients.producer.RecordMetadata
import org.testcontainers.kafka.KafkaContainer
import zio._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde._

object KafkaProducer {

  def produce[T](
    topicName: String,
    key: String,
    value: String
  ): ZIO[
    Producer with KafkaContainer,
    Throwable,
    RecordMetadata,
  ] = {
    for {
      recordMetadata <- Producer.produce[Any, String, String](
                          topic = topicName,
                          key = key,
                          value = value,
                          keySerializer = Serde.string,
                          valueSerializer = Serde.string,
                        )
      _              <- ZIO.logInfo(s"[ZZSpec] Published record with key: $key to Kafka, $recordMetadata")

    } yield recordMetadata
  }

  def layer: ZLayer[KafkaContainer with Scope, Throwable, Producer] =
    ZLayer {
      for {
        kafkaContainer <- ZIO.service[KafkaContainer]
        kafkaServer     = s"${kafkaContainer.getHost}:${kafkaContainer.getMappedPort(9092)}"
        producer       <- Producer.make(
                            settings = ProducerSettings(List(kafkaServer)),
                          )
      } yield producer
    }
}
