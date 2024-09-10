package mockservertest

import zzspec.mockserver.{MockServer, MockServerContainer}
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.http._
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.test._

object MockServerSpec extends ZIOSpecDefault {

  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val logConfig = ConsoleLoggerConfig(
    LogFormat.colored,
    LogFilter.LogLevelByNameConfig.default
  )
  private val logs = Runtime.removeDefaultLoggers >>> consoleLogger(logConfig) >+> Slf4jBridge.initialize

  def spec: Spec[Environment & TestEnvironment & Scope, Any] =
    suite("MockServer tests")(basicMockServerOperations).provideShared(
      Scope.default,
      ZLayer.succeed(Network.SHARED),
      logs,
      ZLayer.succeed(new Slf4jLogConsumer(slf4jLogger)),
      MockServerContainer.layer,
      MockServer.layer,
      Client.default,
    )

  def basicMockServerOperations
    : Spec[Scope & MockServer.Client & MockServerContainer.Container & Client & Scope, Throwable] =
    test("""
      Doing an HTTP request to the mock server returns the expected result.
    """) {
      val someResponseBody = "Legolas, son of Thranduil"
      for {
        mockServerClient <- ZIO.service[MockServer.Client]
        mockServerContainer <- ZIO.service[MockServerContainer.Container]

        _ = mockServerClient.value
          .when(request().withPath("/person").withQueryStringParameter("name", "legolas"))
          .respond(response().withBody(someResponseBody))

        mockServerUrl = new StringBuilder("http://")
          .append(mockServerContainer.value.getHost)
          .append(":")
          .append(mockServerContainer.value.getMappedPort(1080))
          .toString()
        parsedMockServerUrl = URL.decode(mockServerUrl).toOption.get

        httpClient <- ZIO.service[Client]
        res <- httpClient.url(parsedMockServerUrl).get("/person?name=legolas")
        reqBody <- res.body.asString
      } yield assertTrue(reqBody == someResponseBody)
    }
}
