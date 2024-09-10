package postgresqltest

import zzspec.postgresql.PostgreSQL.DDL.{createTable, dropTable, Column => DDLColumn, CreateTable}
import zzspec.postgresql.PostgreSQL.DQL._
import zzspec.postgresql.PostgreSQL._
import zzspec.postgresql._
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import zio._
import zio.logging._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.test._

import java.util.UUID

object PostgreSQLSpec extends ZIOSpecDefault {

  val testTable: CreateTable = CreateTable(
    name = UUID.randomUUID().toString,
    columns = Seq(
      DDLColumn(name = "id", dataType = "VARCHAR NOT NULL PRIMARY KEY"),
      DDLColumn(name = "some_int", dataType = "INT NOT NULL"),
      DDLColumn(name = "some_bool", dataType = "BOOLEAN NOT NULL"),
    ),
  )
  private val slf4jLogger = org.slf4j.LoggerFactory.getLogger("")
  private val logConfig = ConsoleLoggerConfig(
    LogFormat.colored,
    LogFilter.LogLevelByNameConfig.default
  )
  private val logs = Runtime.removeDefaultLoggers >>> consoleLogger(logConfig) >+> Slf4jBridge.initialize

  def spec: Spec[Environment with TestEnvironment with Scope, Any] =
    suite("PostgreSQL query tests")(basicPostgreSQLOperationsTest).provideShared(
      Scope.default,
      ZLayer.succeed(Network.SHARED),
      logs,
      ZLayer.succeed(new Slf4jLogConsumer(slf4jLogger)),
      PostgreSQLContainer.Settings.default,
      PostgreSQLContainer.layer,
      PostgreSQLPool.layer,
    )

  def basicPostgreSQLOperationsTest: Spec[PostgreSQLPool with Scope, Throwable] =
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
    """) {
      for {
        _ <- dropTable(testTable.name)
        _ <- createTable(testTable)

        initialRowCountInTestTable <- countTable(testTable.name)

        _ <- PostgreSQL.updateRaw(s"""
        INSERT INTO "${testTable.name}" (id, some_int, some_bool) VALUES ('a', 1, TRUE);
        INSERT INTO "${testTable.name}" (id, some_int, some_bool) VALUES ('b', 2, FALSE);
        """)

        totalRowCountInTestTable <- countTable(testTable.name)
        constrainedRowCount <- countTable(testTable.name, Seq(Where("id", "=", "a")))
        constrainedRowCount2 <- countTable(testTable.name, Seq(Where("some_int", "=", 1)))
        constrainedRowCount3 <- countTable(testTable.name, Seq(Where("some_bool", "=", true)))
        maybeRowOfId2 <- fetchRow(
          testTable.name,
          Seq(
            Column("id", Decoders.string),
            Column("some_int", Decoders.int),
            Column("some_bool", Decoders.boolean),
          ),
          Seq(Where("id", "=", "b")),
        )
      } yield assertTrue(
        initialRowCountInTestTable == 0,
        totalRowCountInTestTable == 2,
        constrainedRowCount == 1,
        constrainedRowCount2 == 1,
        constrainedRowCount3 == 1,
        maybeRowOfId2.contains(
          Map.from(
            Seq(
              "id" -> "b",
              "some_int" -> 2,
              "some_bool" -> false,
            ),
          ),
        ),
      )
    }
}
