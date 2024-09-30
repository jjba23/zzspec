package io.github.jjba23.zzspec

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.logging.LogFilter
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.process.Command
import zio.test._

import java.io.File
import java.util.UUID

/** ZZSpec provides helper functions and data types to allow you to write
  * high-level and expressive integration and black-box tests with a variety of
  * external services, and leveraging libraries like testcontainers to run a.o.
  * PostgreSQL, Kafka, MockServer, OpenSearch
  */
object ZZSpec {

  /** Parameters for an sbt module run, in other words, running an sbt task from
    * within the integration test itself, process in a process, allowing us to
    * run black box tests on whatever sbt project we want, from the outside,
    * while we become spectators and assert on the side effects produced.
    *
    * @param sbtTask
    * @param moduleName
    * @param env
    * @param testLocation
    */
  case class SbtModuleRun(
    sbtTask: String,
    moduleName: Option[String],
    env: Map[String, String],
    testLocation: String = ".."
  )

  /** Run an sbt task from within the integration test itself, process in a
    * process, allowing us to run black box tests on whatever sbt project we
    * want, from the outside, while we become spectators and assert on the side
    * effects produced.
    *
    * It can be interesting to ".fork" this effect and perform checks in the
    * meantime that the process is running, specially for long-lived processes
    * like REST APIs.
    *
    * @param testCase
    * @return
    */
  def runSbtModule(testCase: SbtModuleRun): ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.logInfo("[ZZSpec] Starting a new module run ...")

      runTaskCmd =
        Command(
          "sbt",
          testCase.moduleName.fold(testCase.sbtTask)(module =>
            s"$module/${testCase.sbtTask}"
          )
        ).copy(
          env = testCase.env,
          workingDirectory = Some(new File(testCase.testLocation))
        )

      _ <- runTaskCmd.inheritIO.exitCode
    } yield ()

  /** Provides a ZLayer with a shared network for the running test and the
    * testcontainers that take part in it.
    */
  val networkLayer: ULayer[Network] = ZLayer.succeed(Network.SHARED)

  /** Provides a ZLayer with the Slf4j log consumer that will be plugged into
    * the testcontainers. This is a separate logger than the test's ZIO/other
    * logger.
    *
    * @param level
    * @return
    */
  def containerLogger(
    level: LogLevel = LogLevel.Warning
  ): ZLayer[Any, Nothing, Slf4jLogConsumer] =
    Slf4jBridge.init(LogFilter.logLevel(level)) >>> ZLayer.succeed(
      new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger(""))
    )

  private val mapper: ObjectMapper = new ObjectMapper()

  /** Attempt to parse a String into a JsonNode with Jackson
    *
    * @param x
    * @return
    */
  def parseJson(x: String): Task[JsonNode] = ZIO.attempt(mapper.readTree(x))

  /** Generate a new UUID v4
    *
    * @return
    */
  def nextRandom: Task[UUID] = ZIO.attempt(java.util.UUID.randomUUID())

  /** Verify that all the log messages satisfy the wanted checks.
    * @param checks
    * @return
    */
  def checkLogs(checks: Set[String => Boolean]): UIO[Boolean] = for {
    loggerOutput <- ZTestLogger.logOutput
                      .map(_.map(_.message()))
    checkResults  = checks.map(f => loggerOutput.find(f).isDefined)
  } yield checkResults.forall(_ == true)
}
