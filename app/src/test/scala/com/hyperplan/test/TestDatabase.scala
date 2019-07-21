package com.hyperplan.test

import doobie._
import cats.effect.IO

import com.hyperplan.infrastructure.storage.PostgresqlService
import org.scalatest.Assertion
import scala.util.Random

trait TestDatabase {

  import scala.concurrent.ExecutionContext
  implicit val cs = IO.contextShift(ExecutionContext.global)

  val dbName: String = Random.alphanumeric.take(10).mkString("")
  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  def transactor = xa

  def initSchema() = PostgresqlService.initSchema(xa).unsafeRunSync()
  def wipeTables() = PostgresqlService.wipeTables(true)(xa).unsafeRunSync

  def withInMemoryDatabase(test: Unit => Unit) = {
    PostgresqlService.initSchema(xa).unsafeRunSync()
    test
  }

}
