package zzspec.kafka

import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import org.testcontainers.utility.DockerImageName
import zio._

object KafkaContainer {

  val layer
    : ZLayer[Slf4jLogConsumer with Network, Throwable, KafkaTestContainer] =
    ZLayer.scoped {
      for {
        logConsumer <- ZIO.service[Slf4jLogConsumer]
        network     <- ZIO.service[Network]
        kafka       <- scopedTestContainer(logConsumer, network)
        _           <- ZIO.logInfo(
                         s"[ZZSpec] Kafka started at: ${kafka.getBootstrapServers}",
                       )
      } yield kafka
    }
  private val image: DockerImageName = DockerImageName
    .parse(
      "docker.io/apache/kafka:3.8.0",
    )
    .asCompatibleSubstituteFor("apache/kafka")

  private def scopedTestContainer(
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any with Scope, KafkaTestContainer] = {
    def prepContainer(container: KafkaTestContainer) =
      ZIO.attempt {
        container.withNetwork(network)
        container.withLogConsumer(logConsumer)
        container.start()
      }

    def containerShutdown(container: KafkaTestContainer) =
      ZIO.attempt(container.stop()).ignoreLogged

    ZIO.acquireRelease(
      ZIO
        .attempt(new KafkaTestContainer(image))
        .tap(prepContainer)
        .orDie,
    )(containerShutdown)
  }
}
