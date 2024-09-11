package zzspec.mockserver

import org.mockserver.client.MockServerClient
import zio.{ZIO, ZLayer}
import zzspec.mockserver.MockServerContainer._

object MockServer {

  val layer: ZLayer[Container, Throwable, Client] = ZLayer {
    for {
      mockServerContainer <- ZIO.service[Container]
    } yield Client(
      new MockServerClient(
        mockServerContainer.value.getHost,
        mockServerContainer.value.getMappedPort(1080)
      ),
    )
  }

  case class Client(value: MockServerClient)
}
