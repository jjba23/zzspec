package zzspec.kafka

import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.{KafkaContainer as KafkaTestContainer, Network}
import org.testcontainers.utility.DockerImageName
import zio.*

object KafkaContainer {

  val layer: ZLayer[Slf4jLogConsumer & Network, Throwable, KafkaTestContainer] =
    ZLayer.scoped {
      for {
        logConsumer <- ZIO.service[Slf4jLogConsumer]
        network <- ZIO.service[Network]
        kafka <- scopedTestContainer(logConsumer, network)
        _ <- ZIO.logInfo(
          s"[BB] Kafka started at: ${kafka.getBootstrapServers}",
        )
      } yield kafka
    }
  private val image: DockerImageName = DockerImageName
    .parse(
      "docker.io/chainguard/kafka:sha256-79a7058bfd4f873582649d77f4f0352bb6c29518925a8b57105b56eaab592b17.sig",
    )
    .asCompatibleSubstituteFor("confluentinc/cp-kafka")

  private def scopedTestContainer(
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any & Scope, KafkaTestContainer] = {
    def prepContainer(container: KafkaTestContainer) =
      ZIO.attempt {
        container.withNetwork(network)
        container.withKraft()
        container.withLogConsumer(logConsumer)
        container.start()
      }

    def containerShutdown(container: KafkaTestContainer) = ZIO.attempt(container.stop()).ignoreLogged

    ZIO.acquireRelease(
      ZIO
        .attempt(new KafkaTestContainer(image))
        .tap(prepContainer)
        .orDie,
    )(containerShutdown)
  }
}
