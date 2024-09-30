package io.github.jjba23.zzspec.slick

import zio._
import slick.jdbc.PostgresProfile.api._

object SlickPostgres {
  def runDB[A](dbio: DBIO[A]): ZIO[Database, Throwable, A] =
    ZIO.serviceWithZIO[Database](db => ZIO.fromFuture { _ => db.run(dbio) })
}
