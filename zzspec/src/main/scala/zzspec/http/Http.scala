package zzspec.http

import zio._
import zio.http._

object Http {

  case class ResponseAssertion(expectedStatusCode: Option[Int], expectedBody: Option[String])

  def assertHttpResponse(
    url: String,
    responseAssertion: ResponseAssertion,
  ): ZIO[Client with Any with Scope, Throwable, Boolean] = {
    val decodedUrl = URL.decode(url).toOption.get

    for {
      client <- ZIO.service[Client]
      _ <- ZIO.logInfo(s"HTTP call to: $decodedUrl")
      res <- client.url(decodedUrl).get("/")
      _ <- ZIO.logInfo(s"HTTP response: $res")
      bodyData <- res.body.asString
      matchesStatusCode = responseAssertion.expectedStatusCode.fold(true)(_ == res.status.code)
      matchesBody = responseAssertion.expectedBody.fold(true)(b => bodyData.trim == b.trim)
    } yield (matchesStatusCode && matchesBody)
  }
}
