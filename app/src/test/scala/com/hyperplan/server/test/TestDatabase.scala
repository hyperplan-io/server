package com.hyperplan.server.test

import doobie._
import cats.effect.IO

import com.hyperplan.infrastructure.storage.PostgresqlService

trait TestDatabase {

  import scala.concurrent.ExecutionContext
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  def transactor = xa

  def withInMemoryDatabase(test: Unit => Unit) = {
    PostgresqlService.initSchema(xa).unsafeRunSync()
    test
  }

}
