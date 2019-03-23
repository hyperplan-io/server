package com.foundaml.server.infrastructure.storage

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import scalaz.zio.{Exit, Task, ZIO}
import scalaz.zio.interop.catz._
import cats.implicits._
import cats.effect.Resource

object PostgresqlService {

  def testConnection(
      implicit xa: doobie.Transactor[Task]
  ): ZIO[Any, Nothing, Exit[Throwable, Double]] =
    sql"select random()".query[Double].unique.transact(xa).run

  def initSchema(implicit xa: doobie.Transactor[Task]) =
    (createProjectsTable, createAlgorithmsTable, createPredictionsTable)
      .mapN(_ + _ + _)
      .transact(xa)

  def apply(
      host: String,
      port: String,
      database: String,
      username: String,
      password: String
  ): Resource[Task, doobie.Transactor[Task]] = {
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

  val createProjectsTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS projects(
        id VARCHAR(36) PRIMARY KEY,
        name VARCHAR NOT NULL,
        algorithm_policy VARCHAR(36) NOT NULL,
        problem VARCHAR NOT NULL,
        features_configuration VARCHAR NOT NULL,
        labels VARCHAR NOT NULL
      )
    """.update.run

  val createAlgorithmsTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS algorithms(
        id VARCHAR(36) PRIMARY KEY,
        backend VARCHAR NOT NULL ,
        project_id VARCHAR(36) NOT NULL,
        FOREIGN KEY (project_id) references projects(id)
      );
    """.update.run

  val createPredictionsTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS predictions(
        id VARCHAR(36) PRIMARY KEY,
        project_id VARCHAR(36) NOT NULL,
        algorithm_id VARCHAR(36) NOT NULL,
        features VARCHAR NOT NULL,
        labels VARCHAR NOT NULL,
        examples VARCHAR NOT NULL
      )
    """.update.run
}
