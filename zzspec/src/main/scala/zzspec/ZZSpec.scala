package zzspec

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.apache.kafka.common.config.TopicConfig
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.process.Command
import zio.test._
import zzspec.kafka.NewTopic

import java.io.File
import java.util.UUID

object ZZSpec {

  case class SbtModuleRun(
    sbtTask: String,
    moduleName: Option[String],
    env: Map[String, String],
    testLocation: String = "..",
  )

  def runSbtModule(testCase: SbtModuleRun): ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.logInfo("[ZZSpec] Starting a new module run ...")

      runTaskCmd =
        Command(
          "sbt",
          testCase.moduleName.fold(testCase.sbtTask)(module => s"$module/${testCase.sbtTask}"),
        ).copy(
          env = testCase.env,
          workingDirectory = Some(new File(testCase.testLocation)),
        )

      _ <- runTaskCmd.inheritIO.exitCode
    } yield ()

  val networkLayer: ULayer[Network] = ZLayer.succeed(Network.SHARED)

  val containerLogger: ZLayer[Any, Nothing, Slf4jLogConsumer] =
    Slf4jBridge.initialize >>> ZLayer.succeed(
      new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger(""))
    )

  private val mapper: ObjectMapper         = new ObjectMapper()
  def parseJson(x: String): Task[JsonNode] = ZIO.attempt(mapper.readTree(x))

  def newTopic(name: String): NewTopic = NewTopic(
    name = name,
    partitions = 1,
    replicationFactor = 1,
    configs = Map(
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT,
    ),
  )

  def newTopic(): Task[NewTopic] = nextRandom.map(uuid => newTopic(uuid.toString))

  def nextRandom: Task[UUID] = ZIO.attempt(java.util.UUID.randomUUID())

  def checkLogs(checks: Set[String => Boolean]): UIO[Boolean] = for {
    loggerOutput <- ZTestLogger.logOutput
                      .map(_.map(_.message()))
    checkResults  = checks.map(f => loggerOutput.find(f).isDefined)
  } yield checkResults.forall(_ == true)
}
