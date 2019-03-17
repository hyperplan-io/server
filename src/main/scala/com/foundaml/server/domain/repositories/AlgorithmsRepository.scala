package com.foundaml.server.domain.repositories

import doobie._
import doobie.implicits._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.domain.models.Algorithm
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.infrastructure.serialization._

class AlgorithmsRepository(implicit xa: Transactor[Task]) {

  implicit val backendGet: Get[Backend] =
    Get[String].map(BackendSerializer.decodeJson)
  implicit val backendPut: Put[Backend] =
    Put[String].contramap(BackendSerializer.encodeJson)

  def insertQuery(algorithm: Algorithm): doobie.Update0 =
    sql"""INSERT INTO algorithms(
      id, 
      backend, 
      project_id
    ) VALUES(
      ${algorithm.id},
      ${algorithm.backend},
      ${algorithm.projectId}
    )""".update

  def insert(algorithm: Algorithm): Task[Int] =
    insertQuery(algorithm).run.transact(xa)

  def readQuery(algorithmId: String): doobie.Query0[Algorithm] =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE id=$algorithmId
      """
      .query[Algorithm]

  def read(algorithmId: String): Task[Algorithm] =
    readQuery(algorithmId).unique.transact(xa)

  def readForProjectQuery(projectId: String): doobie.Query0[Algorithm] =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE project_id=$projectId
      """
      .query[Algorithm]

  def readForProject(projectId: String): Task[List[Algorithm]] =
    readForProjectQuery(projectId).to[List].transact(xa)

  def readAllQuery(): doobie.Query0[Algorithm] =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      """
      .query[Algorithm]

  def readAll(): Task[List[Algorithm]] = readAllQuery().to[List].transact(xa)

}
