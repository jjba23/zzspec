package zzspec.postgresql

import scalikejdbc.*
import zio.*

case class PostgreSQLPool()

object PostgreSQLPool {
  def layer: ZLayer[
    Scope & PostgreSQLContainer.Container,
    Throwable,
    PostgreSQLPool,
  ] =
    ZLayer.fromZIO {
      for {
        postgresqlContainer <- ZIO.service[PostgreSQLContainer.Container]
        _ <- ZIO.attemptBlocking(
          ConnectionPool
            .singleton(
              postgresqlContainer.value.getJdbcUrl,
              postgresqlContainer.value.getUsername,
              postgresqlContainer.value.getPassword,
            ),
        )
      } yield PostgreSQLPool()
    }
}
