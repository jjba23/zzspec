package zzspec.postgresql

import scalikejdbc._
import zio._

case class PostgreSQLPool()

object PostgreSQLPool {
  def layer: ZLayer[
    Scope with PostgreSQLContainer.Container,
    Throwable,
    PostgreSQLPool,
  ] =
    ZLayer.scoped {
      for {
        postgresqlContainer <- ZIO.service[PostgreSQLContainer.Container]
        _                   <- ZIO.attemptBlocking(
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
