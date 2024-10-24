import sbt.Keys._
import sbt._

object Dependencies extends AutoPlugin {

  override def buildSettings: Seq[Def.Setting[?]] = Seq(
    scalaVersion := "2.13.14",
    scalacOptions ++= List(
      "-Yrangepos",
      "-Ywarn-unused"
    ),
    semanticdbEnabled := true,
    semanticdbVersion := "4.9.9"
  )

  override def trigger = allRequirements

  object autoImport {

    val Elastic4sCore = "com.sksamuel.elastic4s" %% "elastic4s-core" % "6.7.8"
    val Elastic4sHttp = "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sCore.revision

    val CirceCore          = "io.circe" %% "circe-core"           % "0.14.10"
    val CirceGeneric       = "io.circe" %% "circe-generic"        % CirceCore.revision
    val CirceGenericExtras = "io.circe" %% "circe-generic-extras" % "0.14.3"
    val CirceLiteral       = "io.circe" %% "circe-literal"        % CirceCore.revision
    val CirceParser        = "io.circe" %% "circe-parser"         % CirceCore.revision

    val KafkaClients = "org.apache.kafka" % "kafka-clients" % "2.8.2"

    val Zio                   = "dev.zio" %% "zio"                       % "2.1.9"
    val ZioTest               = "dev.zio" %% "zio-test"                  % Zio.revision
    val ZioTestMagnolia       = "dev.zio" %% "zio-test-magnolia"         % Zio.revision
    val ZioTestSbt            = "dev.zio" %% "zio-test-sbt"              % Zio.revision
    val ZioPrelude            = "dev.zio" %% "zio-prelude"               % "1.0.0-RC30"
    val ZioProcess            = "dev.zio" %% "zio-process"               % "0.7.2"
    val ZioKafka              = "dev.zio" %% "zio-kafka"                 % "2.8.2"
    val ZioKafkaTest          = "dev.zio" %% "zio-kafka-testkit"         % ZioKafka.revision
    val ZioHttp               = "dev.zio" %% "zio-http"                  % "3.0.0-RC10"
    val ZioLogging            = "dev.zio" %% "zio-logging"               % "2.3.1"
    val ZioLoggingSlf4jBridge = "dev.zio" %% "zio-logging-slf4j2-bridge" % ZioLogging.revision

    val Testcontainers           = "org.testcontainers" % "testcontainers"            % "1.20.1"
    val TestcontainersKafka      = "org.testcontainers" % "kafka"                     % "1.20.1"
    val TestcontainersOpensearch = "org.opensearch"     % "opensearch-testcontainers" % "2.0.1"
    val TestcontainersPostgresql = "org.testcontainers" % "postgresql"                % "1.20.1"
    val TestContainersMockServer = "org.testcontainers" % "mockserver"                % "1.20.1"

    val PostgresqlDriver = "org.postgresql" % "postgresql" % "42.7.4"

    val MockServerClient = "org.mock-server" % "mockserver-client-java" % "5.15.0"

    val JacksonCore        = "com.fasterxml.jackson.core"    % "jackson-core"         % "2.17.2"
    val JacksonDatabind    = "com.fasterxml.jackson.core"    % "jackson-databind"     % JacksonCore.revision
    val JacksonModuleScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonCore.revision

    val Slf4jNop      = "org.slf4j"           % "slf4j-nop"      % "1.7.26"
    val Slick         = "com.typesafe.slick" %% "slick"          % "3.5.1"
    val SlickHikariCP = "com.typesafe.slick" %% "slick-hikaricp" % "3.5.1"

    val ApacheCommonsDBCP = "org.apache.commons" % "commons-dbcp2" % "2.12.0"

    val zzspecDependencyOverrides = List(
      JacksonCore,
      JacksonDatabind,
      JacksonModuleScala,
    )

    val zzspecDependencies: List[ModuleID] = List(
      Zio,
      ZioTest         % Test,
      ZioTestSbt      % Test,
      ZioTestMagnolia % Test,
      ZioPrelude,
      ZioProcess,
      ZioKafka,
      ZioKafkaTest,
      ZioHttp,
      Testcontainers,
      TestcontainersOpensearch,
      TestcontainersPostgresql,
      TestcontainersKafka,
      TestContainersMockServer,
      Slick,
      ApacheCommonsDBCP,
      SlickHikariCP,
      Slf4jNop,
      PostgresqlDriver,
      Elastic4sCore,
      Elastic4sHttp,
      CirceCore,
      CirceGeneric,
      CirceGenericExtras,
      CirceLiteral,
      CirceParser,
      MockServerClient,
      ZioLogging,
      ZioLoggingSlf4jBridge,
    )
  }
}
