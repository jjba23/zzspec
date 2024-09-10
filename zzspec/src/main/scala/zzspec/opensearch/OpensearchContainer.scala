package zzspec.opensearch

import org.opensearch.testcontainers.{OpensearchContainer => OpensearchTestContainer}
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName
import zio._

import scala.jdk.CollectionConverters._

object OpensearchContainer {

  val layer: ZLayer[Settings with Network with Slf4jLogConsumer, Throwable, Container] = ZLayer.scoped {
    for {
      network <- ZIO.service[Network]
      logConsumer <- ZIO.service[Slf4jLogConsumer]
      opensearch <- scopedTestContainer(logConsumer, network)
      _ <- ZIO.logInfo(
        s"[ZZSpec] Opensearch started at: http://${opensearch.getHost}:${opensearch.getMappedPort(9200)})",
      )
    } yield Container(opensearch)
  }
  private val image: DockerImageName = DockerImageName.parse("opensearchproject/opensearch:1.3.13")
  private val defaultSettings = Settings(maxMemoryMb = 1300)

  private def scopedTestContainer(
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any with Scope, OpensearchTestContainer[?]] = {
    def prepContainer(container: OpensearchTestContainer[?]) =
      ZIO.attempt {
        container.withEnv(
          Map.from(Seq("discovery.type" -> "single-node")).asJava,
        )

        container.withLogConsumer(logConsumer)
        container.withNetwork(network)
        container.start()
      }
    def shutdownContainer(container: OpensearchTestContainer[?]) =
      ZIO.attempt(container.stop()).ignoreLogged

    ZIO.acquireRelease(
      ZIO
        .attempt(new OpensearchTestContainer(image))
        .tap(prepContainer)
        .orDie,
    )(shutdownContainer)
  }

  case class Container(value: OpensearchTestContainer[?])

  case class Settings(maxMemoryMb: Int)

  object Settings {

    def default: ULayer[Settings] =
      ZLayer.succeed(defaultSettings)

  }
}
