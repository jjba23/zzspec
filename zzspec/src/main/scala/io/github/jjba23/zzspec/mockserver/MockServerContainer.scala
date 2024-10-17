package io.github.jjba23.zzspec.mockserver

import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.{GenericContainer, Network}
import org.testcontainers.utility.DockerImageName
import zio._

object MockServerContainer {

  def layer(
    image: String = "docker.io/xdevsoftware/mockserver",
    version: String = "1.0.5"
  ): ZLayer[
    Network with Slf4jLogConsumer,
    Throwable,
    Container,
  ] = ZLayer.scoped {
    for {
      network     <- ZIO.service[Network]
      logConsumer <- ZIO.service[Slf4jLogConsumer]
      mockServer  <- scopedTestContainer(
                       DockerImageName
                         .parse(image)
                         .withTag(version),
                       logConsumer,
                       network
                     )

      _ <-
        ZIO.logInfo(
          s"[ZZSpec] MockServer started at: http://${mockServer.getHost}:${mockServer.getMappedPort(1080)})"
        )
    } yield Container(mockServer)
  }

  private def scopedTestContainer(
    dockerImageName: DockerImageName,
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any with Scope, GenericContainer[_]] =
    ZIO.acquireRelease(
      ZIO
        .attempt(
          new GenericContainer(dockerImageName)
        )
        .tap(container =>
          ZIO.attempt(containerSetup(container, logConsumer, network))
        )
        .orDie,
    )(container => ZIO.attempt(container.stop()).ignoreLogged)

  private def containerSetup(
    container: GenericContainer[_],
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): Unit = {
    container.withNetwork(network)
    container.withLogConsumer(logConsumer)
    container.withExposedPorts(1080)
    container.start()
  }

  case class Container(value: GenericContainer[_])
}
