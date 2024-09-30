package io.github.jjba23.zzspec.kafka

import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.kafka.{KafkaContainer => KafkaTestContainer}
import org.testcontainers.utility.DockerImageName
import zio._

/** KafkaContainer exports a layer of a Kafka testcontainer to assist you in
  * creating tests that connect to a real (containerized) instance of Kafka.
  */
object KafkaContainer {

  /** provides a ZLayer of a Kafka testcontainer
    *
    * to use this layer you should provide [Slf4jLogConsumer] and [Network]
    * layers.
    *
    * @param imageName
    *   is the docker image name to be used to create the Kafka testcontainer
    */
  def layer(
    imageName: String = "docker.io/apache/kafka:3.8.0"
  ): ZLayer[Slf4jLogConsumer with Network, Throwable, KafkaTestContainer] =
    ZLayer.scoped {
      for {
        logConsumer <- ZIO.service[Slf4jLogConsumer]
        network     <- ZIO.service[Network]
        kafka       <- scopedTestContainer(logConsumer, network, imageName)
        _           <- ZIO.logInfo(
                         s"[ZZSpec] Kafka started at: ${kafka.getBootstrapServers}",
                       )
      } yield kafka
    }

  private def scopedTestContainer(
    logConsumer: Slf4jLogConsumer,
    network: Network,
    imageName: String
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
        .attempt(
          new KafkaTestContainer(
            DockerImageName
              .parse(
                imageName,
              )
              .asCompatibleSubstituteFor("apache/kafka")
          )
        )
        .tap(prepContainer)
        .orDie,
    )(containerShutdown)
  }
}
