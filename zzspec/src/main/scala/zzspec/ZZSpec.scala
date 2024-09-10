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
import java.nio.file.Path
import java.io.FileWriter

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
  def parseJson(x: String) =
    ZIO.attempt(mapper.readTree(x))

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

  def cleanName = (x: String) =>
    x.strip.toLowerCase().replaceAll("\\s", "-").replaceAll("[^A-Za-z0-9\\-]", "-").replaceAll("-{2,}", "-")

  def fromTestName(
    name: String,
    className: String = this.getClass().toString(),
    extension: String = ".json",
    orElse: Option[String] = None
  ) = {
    val newName = cleanName(name)
    val newClassName = cleanName(className)
    readTestFileOrCreate(s"$newName-$newClassName${extension}", orElse)
  }

  def writeToFile(fileName: String, content: String) = {
    for {
      w <- ZIO.acquireRelease(ZIO.attempt(new FileWriter(fileName)))(w => ZIO.attempt(w.close()).orDie)
      _ <- ZIO.attempt(w.write(content))
    } yield ()
  }

  def readTestFileOrCreate(fileName: String, orElse: Option[String]) = {
    val filePath = Paths.get("zzspec", "src", "main", "resources", "contracts", fileName)

    for {
      fileExists <- ZIO.attempt(Files.exists(filePath)).orElseSucceed(false)
      contents <-
        if (fileExists) {
          ZIO.attempt(Files.readString(filePath))
        } else {
          ZIO.attempt(Files.createFile(filePath)) *> (if (orElse.isDefined) {
                                                        writeToFile(filePath.toString(), orElse.get)
                                                      } else { ZIO.unit }) *> ZIO
            .attempt(Files.readString(filePath))

        }

    } yield contents
  }
}
