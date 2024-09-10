package zzspec.kafka

import org.apache.kafka.clients
import org.apache.kafka.clients.admin.{Admin => AdminClient, AdminClientConfig}
import org.testcontainers.kafka.KafkaContainer
import zio._

import java.util.Properties
import scala.jdk.CollectionConverters._

case class NewTopic(name: String, partitions: Int, replicationFactor: Short, configs: Map[String, String])

object Kafka {

  def deleteTopic(topicName: String): ZIO[KafkaContainer, Throwable, Unit] = deleteTopics(Seq(topicName))
  def deleteTopics(topicNames: Seq[String]): ZIO[KafkaContainer, Throwable, Unit] =
    for {
      kafkaContainer <- ZIO.service[KafkaContainer]
      adminClient = createAdminClient(kafkaContainer.getBootstrapServers)
      _ = adminClient.deleteTopics(topicNames.asJavaCollection)
    } yield ()

  def createTopic(topic: NewTopic): ZIO[KafkaContainer, Throwable, Unit] = createTopics(Seq(topic))
  def createTopics(topics: Seq[NewTopic]): ZIO[KafkaContainer, Throwable, Unit] =
    for {
      kafkaContainer <- ZIO.service[KafkaContainer]
      adminClient = createAdminClient(kafkaContainer.getBootstrapServers)
      javaKafkaTopics = topics.map(toJavaKafkaTopic).asJava
      _ = adminClient.createTopics(javaKafkaTopics)
    } yield ()

  private def toJavaKafkaTopic(t: NewTopic) =
    new clients.admin.NewTopic(t.name, t.partitions, t.replicationFactor).configs(t.configs.asJava)

  private def createAdminClient(bootstrapServers: String): AdminClient = {
    val props = new Properties()
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    AdminClient.create(props)
  }
}
