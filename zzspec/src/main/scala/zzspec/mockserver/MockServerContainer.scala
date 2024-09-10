package zzspec.mockserver

import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.{MockServerContainer as MockServerTestContainer, Network}
import org.testcontainers.utility.DockerImageName
import zio.*

object MockServerContainer {

  val layer: ZLayer[
    Network & Slf4jLogConsumer,
    Throwable,
    Container,
  ] = ZLayer.scoped {
    for {
      network <- ZIO.service[Network]
      logConsumer <- ZIO.service[Slf4jLogConsumer]
      mockServer <- scopedTestContainer(logConsumer, network)
      _ <- ZIO.logInfo(s"[BB] MockServer started at: http://${mockServer.getHost}:${mockServer.getMappedPort(1080)})")
    } yield Container(mockServer)
  }
  private val mockServerVersion = "5.15.0"
  private val image: DockerImageName = DockerImageName
    .parse("mockserver/mockserver")
    .withTag(s"mockserver-$mockServerVersion")

  private def scopedTestContainer(
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any & Scope, MockServerTestContainer] =
    ZIO.acquireRelease(
      ZIO
        .attempt(new MockServerTestContainer(image))
        .tap(container => ZIO.attempt(containerSetup(container, logConsumer, network)))
        .orDie,
    )(container => ZIO.attempt(container.stop()).ignoreLogged)

  private def containerSetup(
    container: MockServerTestContainer,
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): Unit = {
    container.withNetwork(network)
    container.withLogConsumer(logConsumer)
    container.start()
  }

  case class Container(value: MockServerTestContainer)
}
