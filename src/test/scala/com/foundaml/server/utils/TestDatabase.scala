package com.foundaml.server.utils

import com.foundaml.server.infrastructure.storage.PostgresqlService
import doobie.imports.Transactor
import org.scalatest.Assertion
import scalaz.zio.{DefaultRuntime, Task}
import scalaz.zio.interop.catz._

trait TestDatabase extends DefaultRuntime {

  val xa = Transactor.fromDriverManager[Task](
    "org.h2.Driver",
    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  def transactor = xa

  def withInMemoryDatabase(test: Unit => Unit) = {
    unsafeRun(PostgresqlService.initSchema(xa))
    test()
  }

}
