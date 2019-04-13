package com.foundaml.server.test

import com.foundaml.server.infrastructure.storage.PostgresqlService
import doobie._
import scalaz.zio.{DefaultRuntime, Task}
import cats.implicits._

trait TestDatabase extends DefaultRuntime {

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  def transactor = xa

  def withInMemoryDatabase(test: Unit => Unit) = {
    unsafeRun(PostgresqlService.initSchema(xa))
    test
  }

}
