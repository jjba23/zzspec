package postgresqltest

import zio._
import zio.test._
import zzspec.ZZSpec.{containerLogger, networkLayer}
import zzspec.slick.SlickPostgres._
import zzspec.postgresql._
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

object PostgreSQLSpec extends ZIOSpecDefault {

  val testTableName                     = UUID.randomUUID().toString()
  val createTestTable: DBIO[Unit]       = {
    DBIO.seq(
      sqlu"""CREATE TABLE "#${testTableName}" (
        id VARCHAR NOT NULL PRIMARY KEY,
        some_int INT NOT NULL,
        some_bool BOOLEAN NOT NULL
      );"""
    )
  }
  val countTestTable: DBIO[Option[Int]] =
    sql"""SELECT COUNT(1) FROM "#${testTableName}"; """
      .as[Int]
      .headOption

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("PostgreSQL query tests")(basicPostgreSQLOperationsTest)
      .provideShared(
        Scope.default,
        networkLayer,
        containerLogger(),
        PostgreSQLContainer.Settings.default,
        PostgreSQLContainer.layer,
        PostgreSQLPool.layer,
      )

  def basicPostgreSQLOperationsTest: Spec[Database with Scope, Throwable] =
    test("""
    Drop a table.
    Create a table.
    Verify amount of rows is 0.
    Insert 2 rows.
    Verify 2 rows are present.
    Verify querying for a string meets expectation.
    Verify querying for an int meets expectation.
    Verify querying for a boolean meets expectation.
    Verify fetching and parsing a row meets expectation.
    """.strip) {
      for {
        _                          <- runDB(createTestTable)
        _                          <- runDB(DBIO.seq(sqlu"""DROP TABLE "#${testTableName}" """))
        _                          <- runDB(createTestTable)
        initialRowCountInTestTable <- runDB(countTestTable)
        _                          <- runDB(
                                        DBIO.seq(
                                          sqlu"""INSERT INTO "#${testTableName}" (id, some_int, some_bool) VALUES ('a', 1, TRUE);""",
                                          sqlu"""INSERT INTO "#${testTableName}" (id, some_int, some_bool) VALUES ('b', 2, FALSE);"""
                                        )
                                      )

        totalRowCountInTestTable <- runDB(countTestTable)

        constrainedRowCount <-
          runDB(
            sql"""SELECT COUNT(1) FROM "#${testTableName}" WHERE id = 'a'; """
              .as[Int]
              .headOption
          )

        constrainedRowCount2 <-
          runDB(
            sql"""SELECT COUNT(1) FROM "#${testTableName}" WHERE some_int = 1; """
              .as[Int]
              .headOption
          )

        constrainedRowCount3 <-
          runDB(
            sql"""SELECT COUNT(1) FROM "#${testTableName}" WHERE some_bool = TRUE; """
              .as[Int]
              .headOption
          )
        maybeRowOfIdB        <-
          runDB(
            sql"""SELECT id, some_int, some_bool FROM "#${testTableName}" WHERE id = 'b'; """
              .as[(String, Int, Boolean)]
              .headOption
          )

      } yield assertTrue(
        true,
        initialRowCountInTestTable.headOption == Some(0),
        totalRowCountInTestTable.headOption == Some(2),
        constrainedRowCount == Some(1),
        constrainedRowCount2 == Some(1),
        constrainedRowCount3 == Some(1),
        maybeRowOfIdB == Some(("b", 2, false))
      )
    }
}
