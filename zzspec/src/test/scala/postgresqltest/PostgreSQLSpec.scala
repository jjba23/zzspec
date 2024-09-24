package postgresqltest

import zio._
import zio.test._
import zzspec.ZZSpec.{containerLogger, networkLayer}
import zzspec.postgresql._
import slick.jdbc.PostgresProfile._
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

object PostgreSQLSpec extends ZIOSpecDefault {

  val testTableName               = UUID.randomUUID().toString()
  val createTestTable: DBIO[Unit] = {
    DBIO.seq(
      sqlu"""CREATE TABLE ${testTableName} (
        id VARCHAR NOT NULL PRIMARY KEY,
        some_int INT NOT NULL,
        some_bool BOOLEAN NOT NULL
      );"""
    )
  }

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

  def basicPostgreSQLOperationsTest
    : Spec[backend.JdbcDatabaseDef with Scope, Throwable] =
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
        db <- ZIO.service[backend.JdbcDatabaseDef]
        _  <- ZIO.attempt {
                db.run(DBIO.seq(sqlu"DROP TABLE ${testTableName} "))
              }
        _  <- ZIO.attempt { db.run(createTestTable) }

        initialRowCountInTestTable <- ZIO.fromFuture { _ =>
                                        db.run(
                                          sql"SELECT COUNT(1) FROM ${testTableName};"
                                            .as[Int]
                                        )
                                      }

        _ <- ZIO.attempt {
               db.run(
                 DBIO.seq(
                   sqlu"INSERT INTO ${testTableName} (id, some_int, some_bool) VALUES ('a', 1, TRUE);",
                   sqlu"INSERT INTO ${testTableName} (id, some_int, some_bool) VALUES ('b', 2, FALSE);"
                 )
               )
             }

        totalRowCountInTestTable <- ZIO.fromFuture { _ =>
                                      db.run(
                                        sql"SELECT COUNT(1) FROM ${testTableName};"
                                          .as[Int]
                                      )
                                    }

        // constrainedRowCount  <-
        //   countTable(testTable.name, Seq(Where("id", "=", "a")))
        // constrainedRowCount2 <-
        //   countTable(testTable.name, Seq(Where("some_int", "=", 1)))
        // constrainedRowCount3 <-
        //   countTable(testTable.name, Seq(Where("some_bool", "=", true)))
        // maybeRowOfId2        <- fetchRow(
        //                           testTable.name,
        //                           Seq(
        //                             Column("id", Decoders.string),
        //                             Column("some_int", Decoders.int),
        //                             Column("some_bool", Decoders.boolean),
        //                           ),
        //                           Seq(Where("id", "=", "b")),
        //                         )
      } yield assertTrue(
        initialRowCountInTestTable.headOption == Some(0),
        totalRowCountInTestTable.headOption == Some(2)
        // constrainedRowCount == 1,
        // constrainedRowCount2 == 1,
        // constrainedRowCount3 == 1,
        // maybeRowOfId2.contains(
        //   Map.from(
        //     Seq(
        //       "id"        -> "b",
        //       "some_int"  -> 2,
        //       "some_bool" -> false,
        //     ),
        //   )),

      )
    }
}
