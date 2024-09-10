package zzspec.kafka

import org.apache.kafka.clients.producer.RecordMetadata
import org.testcontainers.containers.KafkaContainer
import zio.*
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.*

import scala.jdk.CollectionConverters.*

object KafkaProducer {

  def runProducer[T <: com.google.protobuf.Message](
    topicName: String,
    key: String,
    value: T,
  ): ZIO[
    Producer & KafkaContainer,
    Throwable,
    RecordMetadata,
  ] =
    for {
      serializer =
        new KafkaJsonSerializer[T](
        )
      serializedValue: Array[Byte] = serializer.serialize(topicName, value)
      recordMetadata <- Producer.produce[Any, String, Array[Byte]](
        topic = topicName,
        key = key,
        value = serializedValue,
        keySerializer = Serde.string,
        valueSerializer = Serde.byteArray,
      )
      _ <- ZIO.logInfo(s"[BB] Published record with key: $key to Kafka")

    } yield recordMetadata

  def producerLayer: ZLayer[KafkaContainer & Scope, Throwable, Producer] =
    ZLayer {
      for {
        kafkaContainer <- ZIO.service[KafkaContainer]
        kafkaServer = s"${kafkaContainer.getHost}:${kafkaContainer.getMappedPort(9093)}"
        producer <- Producer.make(
          settings = ProducerSettings(List(kafkaServer)),
        )
      } yield producer
    }
}
