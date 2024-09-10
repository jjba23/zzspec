package zzspec

import org.apache.kafka.common.config.TopicConfig
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.process.Command
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import zzspec.kafka.NewTopic
import java.nio.file.{Files, Paths}
import zio.test._

case class SbtTestCase(
  sbtTask: String,
  moduleName: Option[String],
  env: Map[String, String],
  testLocation: String = "..",
)

object ZZSpec {

  def runSbtTestCase(testCase: SbtTestCase): ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.logInfo("[ZZSpec] Starting a new module run ...")

      runTaskCmd = Command(
        "sbt",
        "-Dconfig.resource=bb.conf",
        testCase.moduleName.fold(testCase.sbtTask)(module => s"$module/${testCase.sbtTask}"),
      ).copy(
        env = testCase.env,
        workingDirectory = Some(new File(testCase.testLocation)),
      )

      _ <- runTaskCmd.inheritIO.exitCode
    } yield ()

  val networkLayer = ZLayer.succeed(Network.SHARED)

  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val loggerConfig = ConsoleLoggerConfig(
    LogFormat.default,
    LogFilter.LogLevelByNameConfig.default
  )
  val containerLogger: ZLayer[Any, Nothing, Slf4jLogConsumer] =
    Runtime.removeDefaultLoggers >>> consoleLogger(loggerConfig) >+> Slf4jBridge.initialize >>> ZLayer.succeed(
      new Slf4jLogConsumer(slf4jLogger)
    )

  val mapper = new ObjectMapper()
  def parseJson(x: String) = mapper.readTree(x)

  def newTopic(name: String): NewTopic = NewTopic(
    name = name,
    partitions = 1,
    replicationFactor = 1,
    configs = Map(
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT,
    ),
  )
  def newTopic(): NewTopic = newTopic(java.util.UUID.randomUUID().toString())

}

object ZZContract {

  def fromTestName(
    name: String,
    className: String = this.getClass().toString(),
    extension: String = ".json"
  ): ZIO[Any, Throwable, String] = {
    def cleanName = (x: String) => x.toLowerCase().replaceAll("\\s", "").replaceAll("[^A-Za-z0-9\\-]", "")
    val newName = cleanName(name)
    val newClassName = cleanName(className)
    readTestFileOrCreate(s"$newClassName-${newName}${extension}")
  }

  def readTestFileOrCreate(fileName: String): ZIO[Any, Throwable, String] = {
    val filePath = Paths.get("src", "main", "resources", "contracts", fileName)

    for {
      fileExists <- ZIO.attempt(Files.exists(filePath)).orElseSucceed(false)
      contents <-
        if (fileExists) {
          ZIO.attempt(Files.readAllBytes(filePath)).map(_.toString)
        } else {
          ZIO
            .attempt(Files.createFile(filePath))
            .flatMap(_ => ZIO.attempt(Files.readAllBytes(filePath)).map(_.toString))
        }
    } yield contents
  }
}
