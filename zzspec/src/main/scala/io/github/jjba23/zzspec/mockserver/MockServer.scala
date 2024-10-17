package io.github.jjba23.zzspec.mockserver

import org.mockserver.client.MockServerClient
import zio.{ZIO, ZLayer}
import io.github.jjba23.zzspec.mockserver.MockServerContainer._

object MockServer {

  val layer: ZLayer[Container, Throwable, Client] = ZLayer.scoped(
    ZIO.serviceWith[Container](container =>
      Client(
        new MockServerClient(
          container.value.getHost,
          container.value.getMappedPort(1080)
        )
      )
    )
  )

  case class Client(value: MockServerClient)
}

