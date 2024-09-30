package io.github.jjba23.zzspec.http

import zio._
import zio.http._

/** Http exports useful functions and data types to help you work with HTTP
  * requests and responses in tests.
  */
object Http {

  /** ExpectedHttpResponse represents an expectation of the result of an HTTP
    * call.
    *
    * @param statusCode
    * @param body
    */
  case class ExpectedHttpResponse(
    statusCode: Option[Int],
    body: Option[String]
  )

  /** doesHttpGetRespondWith allows you to perform an HTTP call to the desired
    * [url] and optionally check that the response matches the expected status
    * code and body.
    *
    * @param url
    *   is the full URL to call
    * @param responseAssertion
    *   contains data to perform assertions with
    */
  def doesHttpGetRespondWith(
    baseUrl: String,
    path: String,
    responseAssertion: ExpectedHttpResponse,
  ): ZIO[Client with Any with Scope, Throwable, Boolean] = {
    val decodedUrl = URL.decode(baseUrl).getOrElse(URL.empty)
    for {
      _                <- ZIO.logInfo(s"HTTP call to: $decodedUrl")
      res              <- ZIO.serviceWithZIO[Client](_.url(decodedUrl).get(path))
      _                <- ZIO.logInfo(s"HTTP response: $res")
      bodyData         <- res.body.asString
      matchesStatusCode =
        responseAssertion.statusCode.fold(true)(_ == res.status.code)
      matchesBody       =
        responseAssertion.body.fold(true)(b => bodyData.trim == b.trim)
    } yield (matchesStatusCode && matchesBody)
  }
}
