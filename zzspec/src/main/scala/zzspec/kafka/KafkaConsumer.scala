package zzspec.kafka

import org.apache.kafka.clients.producer.RecordMetadata
import org.testcontainers.kafka.KafkaContainer
import zio._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde._

import org.apache.kafka.clients
import org.apache.kafka.clients.admin.{Admin => AdminClient, AdminClientConfig}
import org.testcontainers.kafka.KafkaContainer
import zio._
import zio.kafka.consumer._
import java.util.Properties
import scala.jdk.CollectionConverters._
import zio.kafka.serde.Serde
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import zio.kafka.testkit
import zio.kafka.consumer.Consumer.OffsetRetrieval
import zio.kafka.consumer.Consumer.AutoOffsetStrategy

import scala.jdk.CollectionConverters._

object KafkaConsumer {

  def layer: ZLayer[KafkaContainer with Scope, Throwable, Consumer] = ZLayer.fromZIO {
    for {
      kafkaContainer <- ZIO.service[KafkaTestContainer]
      consumer <- Consumer.make(
        ConsumerSettings(
          bootstrapServers = List(kafkaContainer.getBootstrapServers())
        ).withGroupId("zzspec")
          .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
          .withPollTimeout(100.millis)
      )
    } yield consumer
  }
}
