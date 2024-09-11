package zzspec.postgresql

import scalikejdbc._
import zio.{Scope, Task, ZIO}
import zzspec.postgresql.PostgreSQL.DQL._

import java.time.ZonedDateTime
import java.util.Date

object PostgreSQL {

  private type DBEff[T] = ZIO[PostgreSQLPool with Scope, Throwable, T]

  def updateRaw(sql: String): DBEff[Int] = ZIO.attemptBlocking {
    DB.localTx { implicit session =>
      SQL(sql).update()
    }
  }

  def executeRaw(sql: String): DBEff[Boolean] = ZIO.attemptBlocking {
    DB.autoCommit { implicit session =>
      SQL(sql).execute()
    }
  }

  def countTable(tableName: String): DBEff[Long] = countTable(tableName, Seq.empty)

  def countTable(tableName: String, constraints: Seq[Where]): DBEff[Long] = ZIO.attemptBlocking {
    val where = makeWhereConstraints(constraints)
    val stmt  = s"""
      SELECT COUNT(1) as bb_count FROM "$tableName" WHERE $where;
    """

    val count = DB.readOnly { implicit session =>
      SQL(stmt)
        .map(_.long("bb_count"))
        .single()
    }

    count.fold(0.toLong)(identity)
  }

  private def makeWhereConstraints(constraints: Seq[Where]): String = {
    def buildConstraint(constraint: Where): String = {
      val encodedValue = constraint.value match {
        case x: Boolean => Encoders.boolean(x)
        case x: Int     => Encoders.int(x)
        case x: Long    => Encoders.long(x)
        case x: Double  => Encoders.double(x)
        case x: String  => Encoders.string(x)
        case x: Any     => Encoders.string(x.toString)
      }
      s"${constraint.name} ${constraint.operator} $encodedValue"
    }

    val allConstraints = Seq(Where("1", "=", 1)) ++ constraints
    allConstraints
      .map(buildConstraint)
      .mkString(" AND ")
  }

  def fetchRow(
    tableName: String,
    colsToFetch: Seq[Column],
  ): DBEff[MaybeRow] = fetchRow(tableName, colsToFetch, Seq.empty)

  def fetchRow(
    tableName: String,
    columnsToFetch: Seq[Column],
    constraints: Seq[Where],
  ): Task[MaybeRow] = ZIO.attemptBlocking {
    val columns: String = makeSerializedColumnNames(columnsToFetch)
    val where: String   = makeWhereConstraints(constraints)

    val stmt = s"""
    SELECT $columns FROM "$tableName" WHERE $where LIMIT 1;
    """

    val row = DB.readOnly { implicit session =>
      SQL(stmt)
        .map { rs =>
          val parsedColumns = columnsToFetch
            .map(columnParse(rs))
            .map(col => (col.name -> col.value))
          Map.from(parsedColumns)
        }
        .single()
    }
    row
  }

  private def makeSerializedColumnNames(columnsToFetch: Seq[Column]): String =
    columnsToFetch.map(_.name).mkString(", ")

  private def columnParse(rs: WrappedResultSet)(columnFetch: Column): ParseResult =
    ParseResult(name = columnFetch.name, value = columnFetch.parser.eff(rs)(columnFetch.name))

  object DDL {

    def createTable(create: CreateTable): DBEff[Boolean] = {
      def makeColumnDef(c: Column): String = s"${c.name} ${c.dataType}"
      executeRaw(s"""
      CREATE TABLE "${create.name}" (${create.columns.map(makeColumnDef).mkString(", ")});
      """)
    }

    def dropTable(table: String): DBEff[Boolean] = dropTables(Seq(table))

    def dropTables(tables: Seq[String]): DBEff[Boolean] = {
      val tablesStr = tables.map(x => s""""$x"""").mkString(", ")
      executeRaw(s"""DROP TABLE IF EXISTS $tablesStr CASCADE;""")
    }

    case class CreateTable(name: String, columns: Seq[Column])

    case class Column(name: String, dataType: String)
  }

  object DQL {

    type MaybeRow = Option[Map[String, Any]]

    case class Where(name: String, operator: String, value: Any)

    case class ColumnParser(eff: WrappedResultSet => String => Any)

    case class Column(name: String, parser: ColumnParser)

    case class ParseResult(name: String, value: Any)

    object Decoders {

      def string: ColumnParser = build(_.string)

      def int: ColumnParser = build(_.int)

      def long: ColumnParser = build(_.long)

      def double: ColumnParser = build(_.double)

      private def build: (WrappedResultSet => String => Any) => ColumnParser = ColumnParser.apply

      def boolean: ColumnParser = build(_.boolean)

      def date: ColumnParser = build(_.date)

      def zonedDateTime: ColumnParser = build(_.zonedDateTime)
    }
  }

  object Encoders {

    def string(x: String): String               = s"'$x'"
    def int(x: Int): String                     = s"$x"
    def long(x: Long): String                   = s"$x"
    def double(x: Double): String               = s"$x"
    def boolean(x: Boolean): String             = if (x) "TRUE" else "FALSE"
    def date(x: Date): String                   = s"'$x'"
    def zonedDateTime(x: ZonedDateTime): String = s"'$x'"
  }
}
