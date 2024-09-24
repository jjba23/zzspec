package zzspec.postgresql

import zio._
import slick.jdbc.PostgresProfile.api._
import org.apache.commons.dbcp2.BasicDataSource
import javax.sql.DataSource
import slick.jdbc.PostgresProfile

object PostgreSQLPool {
  def layer: ZLayer[
    PostgreSQLContainer.Container,
    Throwable,
    PostgresProfile.backend.JdbcDatabaseDef
  ] =
    ZLayer.scoped {
      for {
        postgresqlContainer <- ZIO.service[PostgreSQLContainer.Container]

        dbConf <- ZIO.attemptBlocking {
                    val dataSource: DataSource = {
                      val ds = new BasicDataSource
                      ds.setDriverClassName(
                        "org.postgresql.ds.PGSimpleDataSource"
                      )
                      ds.setUsername(postgresqlContainer.value.getUsername())
                      ds.setPassword(postgresqlContainer.value.getPassword())
                      ds.setMaxTotal(30);
                      ds.setMaxIdle(10);
                      ds.setInitialSize(10);
                      ds.setValidationQuery(
                        "SELECT 1 + 1"
                      )
                      new java.io.File(
                        "target"
                      ).mkdirs // ensure that folder for database exists
                      ds.setUrl(postgresqlContainer.value.getJdbcUrl())
                      ds
                    }

                    dataSource.getConnection().close()

                    val database = Database.forDataSource(dataSource, Some(30))
                    database
                  }
      } yield dbConf
    }
}
