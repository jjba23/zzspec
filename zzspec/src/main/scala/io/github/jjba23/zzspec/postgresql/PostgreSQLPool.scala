package io.github.jjba23.zzspec.postgresql

import zio._
import slick.jdbc.PostgresProfile.api._
import org.apache.commons.dbcp2.BasicDataSource

object PostgreSQLPool {
  def layer: ZLayer[
    PostgreSQLContainer.Container,
    Throwable,
    Database
  ] =
    ZLayer.scoped {
      for {
        postgresqlContainer <- ZIO.service[PostgreSQLContainer.Container]

        dataSource = {
          val ds = new BasicDataSource
          ds.setDriverClassName(
            "org.postgresql.Driver"
          )
          ds.setUsername(postgresqlContainer.value.getUsername())
          ds.setPassword(postgresqlContainer.value.getPassword())
          ds.setMaxTotal(30);
          ds.setMaxIdle(10);
          ds.setInitialSize(10);
          ds.setValidationQuery(
            "SELECT 1 + 1"
          )
          ds.setUrl(postgresqlContainer.value.getJdbcUrl())
          ds
        }

        db <- ZIO.acquireRelease {
                // validate opening conn works
                ZIO.attempt(dataSource.getConnection().close()) *>
                ZIO.succeed(Database.forDataSource(dataSource, Some(10)))
              }(db => ZIO.succeed(db.close()))
      } yield db
    }
}
