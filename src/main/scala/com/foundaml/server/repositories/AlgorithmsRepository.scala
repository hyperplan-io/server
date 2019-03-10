package com.foundaml.server.repositories

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models.Algorithm
import com.foundaml.server.models.backends._

import com.foundaml.server.services.infrastructure.serialization._

import io.circe.generic.extras.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.extras.Configuration, io.circe.generic.extras.auto._

class AlgorithmsRepository(implicit xa: Transactor[Task]) {
  
    implicit val discriminator: Configuration = CirceEncoders.discriminator 

    implicit val backendGet: Get[Backend] = Get[String].map(BackendSerializer.fromString)
    implicit val backendPut: Put[Backend] = Put[String].contramap(BackendSerializer.toString)

  def insert(algorithm: Algorithm) =
    sql"""INSERT INTO algorithms(
      id, 
      backend, 
      project_id
    ) VALUES(
      ${algorithm.id},
      ${algorithm.backend},
      ${algorithm.projectId}
    )""".update

  def read(algorithmId: String) =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE id=${algorithmId}
      """
      .query[Algorithm]

  def readForProject(projectId: String) =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE project_id=${projectId}
      """
      .query[Algorithm]

  def readAll() =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      """
      .query[Algorithm]

}
