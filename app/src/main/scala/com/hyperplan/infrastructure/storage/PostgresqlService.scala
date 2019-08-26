/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.infrastructure.storage

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import cats.implicits._
import cats.effect.{IO, Resource}
import cats.effect.ContextShift

object PostgresqlService {

  val predictionsTable = "predictions"
  val projectsTable = "projects"
  val algorithmsTable = "algorithms"
  val featuresTable = "features"
  val labelsTable = "labels"
  val entityLinksTable = "entity_links"

  val allTables = List(
    entityLinksTable,
    predictionsTable,
    algorithmsTable,
    projectsTable,
    featuresTable,
    labelsTable
  )

  def testConnection(
      implicit xa: doobie.Transactor[IO]
  ): IO[Double] =
    sql"select random()".query[Double].unique.transact(xa)

  def initSchema(implicit xa: doobie.Transactor[IO]) =
    (
      createProjectsTable,
      createAlgorithmsTable,
      createPredictionsTable,
      createFeaturesTable,
      createLabelsTable,
      createEntityLinksTable
    ).mapN(_ + _ + _ + _ + _ + _)
      .transact(xa)

  def wipeTables(areYouSure: Boolean)(implicit xa: doobie.Transactor[IO]) =
    if (areYouSure)
      (
        sql"DELETE from entity_links".update.run,
        sql"DELETE from predictions".update.run,
        sql"DELETE from algorithms".update.run,
        sql"DELETE from projects".update.run,
        sql"DELETE from features".update.run,
        sql"DELETE from labels".update.run
      ).mapN(_ + _ + _ + _ + _ + _).transact(xa)
    else IO.unit

  def apply(
      host: String,
      port: String,
      database: String,
      username: String,
      password: String,
      schema: String,
      threadPool: Int
  )(implicit cs: ContextShift[IO]): Resource[IO, doobie.Transactor[IO]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](threadPool) // our connect EC
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://$host:$port/$database?currentSchema=$schema",
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
        algorithm_policy VARCHAR NOT NULL,
        features_id VARCHAR NOT NULL,
        labels_id VARCHAR,
        topic VARCHAR,
        problem VARCHAR NOT NULL,
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP,
        deleted_at TIMESTAMP
      )
    """.update.run

  val createAlgorithmsTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS algorithms(
        id VARCHAR NOT NULL,
        backend VARCHAR NOT NULL,
        project_id VARCHAR(36) NOT NULL,
        security VARCHAR NOT NULL,
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP,
        deleted_at TIMESTAMP,
        PRIMARY KEY (id, project_id),
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

  val createEntityLinksTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS entity_links(
        prediction_id VARCHAR(36) NOT NULL,
        entity_name VARCHAR NOT NULL,
        entity_id VARCHAR NOT NULL
      )
    """.update.run

  val createFeaturesTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS features(
        id VARCHAR(36) PRIMARY KEY,
        data VARCHAR NOT NULL,
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP,
        deleted_at TIMESTAMP
      )
    """.update.run

  val createLabelsTable: doobie.ConnectionIO[Int] = sql"""
      CREATE TABLE IF NOT EXISTS labels(
        id VARCHAR(36) PRIMARY KEY,
        data VARCHAR NOT NULL,
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP,
        deleted_at TIMESTAMP
      )
    """.update.run

}
