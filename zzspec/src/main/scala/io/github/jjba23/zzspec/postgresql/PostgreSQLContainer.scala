package io.github.jjba23.zzspec.postgresql

import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.{Network, PostgreSQLContainer => PostgreSQLTestContainer}
import org.testcontainers.utility.DockerImageName
import zio._

object PostgreSQLContainer {

  def layer(
    image: DockerImageName = DockerImageName
      .parse(
        "docker.io/postgres:15",
      )
      .asCompatibleSubstituteFor("postgres")
  ): ZLayer[
    Settings with Network with Slf4jLogConsumer,
    Throwable,
    Container,
  ] = ZLayer.scoped {
    for {
      settings    <- ZIO.service[Settings]
      network     <- ZIO.service[Network]
      logConsumer <- ZIO.service[Slf4jLogConsumer]
      postgresql  <- scopedTestContainer(image, settings, logConsumer, network)
      _           <-
        ZIO.logInfo(
          s"[ZZSpec] PostgreSQL started at: http://${postgresql.getHost}:${postgresql.getMappedPort(5432)})"
        )
    } yield Container(postgresql)
  }

  private def scopedTestContainer(
    image: DockerImageName,
    settings: Settings,
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): URIO[Any with Scope, PostgreSQLTestContainer[?]] =
    ZIO.acquireRelease(
      ZIO
        .attempt(new PostgreSQLTestContainer(image))
        .tap(container =>
          ZIO.attempt(containerSetup(container, settings, logConsumer, network))
        )
        .orDie,
    )(container => ZIO.attempt(container.stop()).ignoreLogged)

  private def containerSetup(
    container: PostgreSQLTestContainer[?],
    settings: Settings,
    logConsumer: Slf4jLogConsumer,
    network: Network,
  ): Unit = {
    container.withDatabaseName(settings.databaseName)
    container.withPassword(settings.password)
    container.withUsername(settings.username)
    container.withNetwork(network)
    container.withLogConsumer(logConsumer)
    container.start()
  }

  case class Container(value: PostgreSQLTestContainer[?])

  case class Settings(username: String, password: String, databaseName: String)

  object Settings {
    def layer(
      settings: Settings = Settings(
        username = "zzspec",
        password = "zzspec",
        databaseName = "zzspec",
      )
    ): ULayer[Settings] =
      ZLayer.succeed(settings)
  }
}
