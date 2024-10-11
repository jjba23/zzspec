package io.github.jjba23.zzspec.slick

import zio._
import slick.jdbc.JdbcBackend
import slick.dbio.DBIOAction
import slick.dbio.NoStream

object SlickPostgreSQL {
  def runDB[R](
    dbio: DBIOAction[R, NoStream, Nothing]
  ): ZIO[JdbcBackend.Database, Throwable, R] =
    ZIO.serviceWithZIO[JdbcBackend.Database](db =>
      ZIO.fromFuture { _ => db.run(dbio) }
    )
}
