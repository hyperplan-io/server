package com.foundaml.server.services.infrastructure.storage

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import cats.implicits._

object PostgresqlService {

  def testConnection(implicit xa: HikariTransactor[Task]) =
    sql"select random()".query[Double].unique.transact(xa).run

  def apply(
      host: String,
      port: String,
      database: String,
      username: String,
      password: String
  ) = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[Task](32) // our connect EC
      te <- ExecutionContexts.cachedThreadPool[Task]
      xa <- HikariTransactor.newHikariTransactor[Task](
        "org.postgresql.Driver",
        s"jdbc:postgresql://$host:$port/$database",
        username,
        password,
        ce,
        te
      )
    } yield xa
  }
}
