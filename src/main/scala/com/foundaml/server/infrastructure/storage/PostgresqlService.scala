package com.foundaml.server.infrastructure.storage

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import cats.implicits._
import cats.effect.{ IO, Resource }

object PostgresqlService {

  def testConnection(
      implicit xa: doobie.Transactor[IO]
  ): IO[Double] =
    sql"select random()".query[Double].unique.transact(xa)

  def initSchema(implicit xa: doobie.Transactor[IO]) =
    (createProjectsTable, createAlgorithmsTable, createPredictionsTable)
      .mapN(_ + _ + _)
      .transact(xa)

  import cats.effect.ContextShift
  def apply(
      host: String,
      port: String,
      database: String,
      username: String,
      password: String
  )(implicit cs: ContextShift[IO]): Resource[IO, doobie.Transactor[IO]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
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
        configuration VARCHAR NOT NULL,
        problem VARCHAR NOT NULL
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
        type VARCHAR(50) NOT NULL,
        algorithm_id VARCHAR(36) NOT NULL,
        features VARCHAR NOT NULL,
        labels VARCHAR NOT NULL,
        examples VARCHAR NOT NULL
      )
    """.update.run
}
