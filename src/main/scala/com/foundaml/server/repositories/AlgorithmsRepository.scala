package com.foundaml.server.repositories

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models.Algorithm

class AlgorithmsRepository(implicit xa: Transactor[Task]) {

  def insert(algorithm: Algorithm) =
    sql"""INSERT INTO algorithms(
      id, 
      backend, 
      project_id
    ) VALUES(
      ${algorithm.id},
      ${algorithm.backend.toString},
      ${algorithm.projectId}
    )""".update

  def read(algorithmId: String) =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE id=${algorithmId}
      """
      .query[(String, String, String)]

  def readForProject(projectId: String) =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      WHERE project_id=${projectId}
      """
      .query[(String, String, String)]

  def readAll() =
    sql"""
      SELECT id, backend, project_id 
      FROM algorithms 
      """
      .query[(String, String, String)]


}
