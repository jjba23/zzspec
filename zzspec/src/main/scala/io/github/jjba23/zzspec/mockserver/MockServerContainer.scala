package io.github.jjba23.zzspec.mockserver

import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.{GenericContainer, Network}
import org.testcontainers.utility.DockerImageName
import zio._

object MockServerContainer {

  val layer: ZLayer[
    Network with Slf4jLogConsumer,
    Throwable,
    Container,
  ] = ZLayer.scoped {
    for {
      network     <- ZIO.service[Network]
      logConsumer <- ZIO.service[Slf4jLogConsumer]
      mockServer  <- scopedTestContainer(logConsumer, network)
      _           <-
        ZIO.logInfo(
          s"[ZZSpec] MockServer started at: http://${mockServer.getHost}:${mockServer.getMappedPort(1080)})"
        )
    } yield Container(mockServer)
  }
  private val mockServerVersion      = "latest"
  private val image: DockerImageName = DockerImageName
    .parse("docker.io/xdevsoftware/mockserver")
    .withTag(mockServerVersion)

  private def scopedTestContainer(
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any with Scope, GenericContainer[_]] =
    ZIO.acquireRelease(
      ZIO
        .attempt(new GenericContainer(image))
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
