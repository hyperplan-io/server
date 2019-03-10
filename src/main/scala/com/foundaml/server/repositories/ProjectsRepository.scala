package com.foundaml.server.repositories

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models.Project

class ProjectsRepository(implicit xa: Transactor[Task]) {

  def insert(project: Project) =
    sql"""INSERT INTO projects(
      id, 
      name, 
      problem, 
      algorithm_policy, 
      feature_class, 
      label_class
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.problem.toString},
      ${project.policy.toString},
      ${project.featureType.toString},
      ${project.labelType.toString}
    )""".update

  def read(projectId: String) =
    sql"""
      SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      WHERE id=${projectId}
      """
      .query[(String, String, String, String, String, String)]

  def readAll() =
    sql"""
      SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      """
      .query[(String, String, String, String, String, String)]

}
