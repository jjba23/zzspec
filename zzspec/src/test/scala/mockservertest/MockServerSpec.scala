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
import zzspec.ZZSpec.networkLayer
import zzspec.ZZSpec.containerLogger

object MockServerSpec extends ZIOSpecDefault {

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("MockServer tests")(basicMockServerOperations).provideShared(
      Scope.default,
      networkLayer,
      containerLogger,
      MockServerContainer.layer,
      MockServer.layer,
      Client.default,
    ) @@ TestAspect.withLiveClock

  def basicMockServerOperations: Spec[
    Scope with MockServer.Client with MockServerContainer.Container with Client with Scope,
    Throwable
  ] =
    test("""
      Doing an HTTP request to the mock server returns the expected result.
    """.strip) {
      val someResponseBody = "Legolas, son of Thranduil"
      for {
        mockServerClient    <- ZIO.service[MockServer.Client]
        mockServerContainer <- ZIO.service[MockServerContainer.Container]

        _ = mockServerClient.value
              .when(request().withPath("/person").withQueryStringParameter("name", "legolas"))
              .respond(response().withBody(someResponseBody))

        mockServerUrl       = new StringBuilder("http://")
                                .append(mockServerContainer.value.getHost)
                                .append(":")
                                .append(mockServerContainer.value.getMappedPort(1080))
                                .toString()
        parsedMockServerUrl = URL.decode(mockServerUrl).toOption.get

        res     <- ZIO.serviceWithZIO[Client](_.url(parsedMockServerUrl).get("/person?name=legolas"))
        reqBody <- res.body.asString
      } yield assertTrue(reqBody == someResponseBody)
    }
}
