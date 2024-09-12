package zzspec.kafka

import org.apache.kafka.clients
import org.apache.kafka.clients.admin.{Admin => AdminClient, AdminClientConfig}
import org.apache.kafka.clients.producer.RecordMetadata
import org.testcontainers.kafka.KafkaContainer
import zio._
import zio.kafka.producer.Producer
import zio.kafka.serde._

import java.util.Properties
import scala.jdk.CollectionConverters._

/** Kafka provides helpers and utilities for working, modifying preparing, and
  * testing Kafka in a testcontainer
  */
object Kafka {

  /** NewTopic contains details to create a new Kafka topic
    *
    * @param name
    * @param partitions
    * @param replicationFactor
    * @param configs
    */
  case class NewTopic(
    name: String,
    partitions: Int,
    replicationFactor: Short,
    configs: Map[String, String]
  )

  /** delete a Kafka topic from the testcontainer
    *
    * @param topicName
    */
  def deleteTopic(topicName: String) = deleteTopics(Seq(topicName))

  /** delete Kafka topics from the testcontainer
    *
    * @param topicNames
    */
  def deleteTopics(topicNames: Seq[String]): ZIO[
    KafkaContainer with KafkaContainer,
    Throwable,
    clients.admin.DeleteTopicsResult
  ] =
    doWithAdminClient(admin => admin.deleteTopics(topicNames.asJavaCollection))

  /** create a Kafka topic in the testcontainer
    *
    * @param topic
    */
  def createTopic(topic: NewTopic) = createTopics(Seq(topic))

  /** create Kafka topics in the testcontainer
    *
    * @param topics
    */
  def createTopics(topics: Seq[NewTopic]): ZIO[
    KafkaContainer with KafkaContainer,
    Throwable,
    clients.admin.CreateTopicsResult
  ] = doWithAdminClient(admin =>
    admin.createTopics(topics.map(toJavaKafkaTopic).asJava)
  )

  private def doWithAdminClient[T](eff: AdminClient => T) =
    ZIO.serviceWithZIO[KafkaContainer](kafkaContainer =>
      ZIO.acquireReleaseWith(
        createAdminClient(kafkaContainer.getBootstrapServers)
      )(admin => ZIO.succeed(admin.close()))(admin => ZIO.attempt(eff(admin)))
    )

  /** produce provides a simple way to publish messages (of string key and body)
    * to a Kafka topic in the testcontainer
    *
    * @param topicName
    * @param key
    * @param value
    */
  def produce(topicName: String)(key: String, value: String): ZIO[
    Producer with KafkaContainer,
    Throwable,
    RecordMetadata
  ] = {
    for {
      recordMetadata <- Producer.produce[Any, String, String](
                          topic = topicName,
                          key = key,
                          value = value,
                          keySerializer = Serde.string,
                          valueSerializer = Serde.string
                        )
      _              <-
        ZIO.logInfo(
          s"[ZZSpec] Published record with key: $key and value: $value to Kafka"
        )
      _              <- ZIO.logInfo(s"[ZZSpec] record metadata: $recordMetadata")

    } yield recordMetadata
  }

  private def toJavaKafkaTopic(t: NewTopic) =
    new clients.admin.NewTopic(t.name, t.partitions, t.replicationFactor)
      .configs(t.configs.asJava)

  private def createAdminClient(bootstrapServers: String): Task[AdminClient] = {
    val props = new Properties()
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    ZIO.attempt(AdminClient.create(props))
  }
}
