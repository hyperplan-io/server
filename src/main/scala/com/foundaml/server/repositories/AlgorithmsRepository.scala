package com.foundaml.server.repositories

import doobie._
import doobie.implicits._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models.Algorithm
import com.foundaml.server.models.backends._
import com.foundaml.server.services.infrastructure.serialization._


class AlgorithmsRepository(implicit xa: Transactor[Task]) {

    implicit val backendGet: Get[Backend] = Get[String].map(BackendSerializer.decodeJson)
    implicit val backendPut: Put[Backend] = Put[String].contramap(BackendSerializer.encodeJson)

  def insert(algorithm: Algorithm): doobie.Update0 =
    sql"""INSERT INTO algorithms(
      id, 
      backend, 
      project_id
    ) VALUES(
      ${algorithm.id},
      ${algorithm.backend},
      ${algorithm.projectId}
    )""".update

  def read(algorithmId: String): doobie.Query0[Algorithm] =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE id=$algorithmId
      """
      .query[Algorithm]

  def readForProject(projectId: String): doobie.Query0[Algorithm] =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE project_id=$projectId
      """
      .query[Algorithm]

  def readAll(): doobie.Query0[Algorithm] =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      """
      .query[Algorithm]

}
