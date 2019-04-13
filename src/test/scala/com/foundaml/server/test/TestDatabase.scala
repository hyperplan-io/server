package com.foundaml.server.test

import com.foundaml.server.infrastructure.storage.PostgresqlService
import doobie._
import cats.implicits._
import cats.effect.IO

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
