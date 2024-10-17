package mockservertest

import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import zio._
import zio.http._
import zio.test._
import io.github.jjba23.zzspec.ZZSpec.{containerLogger, networkLayer}
import io.github.jjba23.zzspec.http.Http._
import io.github.jjba23.zzspec.mockserver.{MockServer, MockServerContainer}

object MockServerSpec extends ZIOSpecDefault {

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("MockServer tests")(basicMockServerOperations).provideShared(
      Scope.default,
      networkLayer,
      containerLogger(),
      MockServerContainer.layer(),
      MockServer.layer,
      Client.default,
    ) @@ TestAspect.withLiveClock

  def basicMockServerOperations: Spec[
    Scope
      with MockServer.Client
      with MockServerContainer.Container
      with Client
      with Scope,
    Throwable
  ] =
    test("""
      Doing an HTTP request to the mock server returns the expected result.
    """.strip) {
      val someResponseBody =
        "ZZSpec tests help you grow your confidence in the correctness of software"
      for {
        mockServerClient    <- ZIO.service[MockServer.Client]
        mockServerContainer <- ZIO.service[MockServerContainer.Container]

        _ = mockServerClient.value
              .when(
                request()
                  .withPath("/some-endpoint")
                  .withQueryStringParameter("offset", "1000")
              )
              .respond(response().withBody(someResponseBody))

        mockServerUrl = new StringBuilder("http://")
                          .append(mockServerContainer.value.getHost)
                          .append(":")
                          .append(mockServerContainer.value.getMappedPort(1080))
                          .toString()

        _ <- ZIO.logInfo(
               s"[ZZSpec] mock server is running at $mockServerUrl"
             )

        matchesHttpExpectation <-
          doesHttpGetRespondWith(
            baseUrl = mockServerUrl,
            path = "/some-endpoint?offset=1000",
            responseAssertion = ExpectedHttpResponse(
              statusCode = Some(200),
              body = Some(someResponseBody)
            )
          )
      } yield assertTrue(matchesHttpExpectation)
    }
}
