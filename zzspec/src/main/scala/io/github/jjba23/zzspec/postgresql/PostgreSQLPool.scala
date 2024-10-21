package io.github.jjba23.zzspec.postgresql

import zio._
import org.apache.commons.dbcp2.BasicDataSource
import slick.jdbc.JdbcBackend

object PostgreSQLPool {
  def layer(
    schema: String = "public",
    validationQuery: String = "SELECT 1 + 1"
  ): ZLayer[
    PostgreSQLContainer.Container,
    Throwable,
    JdbcBackend.Database
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
          ds.setMaxTotal(30)
          ds.setMaxIdle(10)
          ds.setInitialSize(10)
          ds.setDefaultSchema(schema)
          ds.setValidationQuery(
            validationQuery
          )
          ds.setUrl(postgresqlContainer.value.getJdbcUrl())
          ds
        }

        db <- ZIO.acquireRelease {
                // validate opening conn works
                ZIO.attempt(dataSource.getConnection().close()) *>
                ZIO.succeed(
                  JdbcBackend.Database.forDataSource(dataSource, Some(10))
                )
              }(db => ZIO.succeed(db.close()))
      } yield db
    }
}
