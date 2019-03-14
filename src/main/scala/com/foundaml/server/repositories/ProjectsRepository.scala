package com.foundaml.server.repositories


import doobie._
import doobie.implicits._

import io.circe.generic.extras.auto._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models._
import com.foundaml.server.services.infrastructure.serialization._


class ProjectsRepository(implicit xa: Transactor[Task]) {


    implicit val problemTypeGet: Get[Either[io.circe.Error,ProblemType]] = Get[String].map(ProblemTypeSerializer.decodeJson)
    implicit val problemTypePut: Put[ProblemType] = Put[String].contramap(ProblemTypeSerializer.encodeJson)

    implicit val algorithmPolicyTypeGet: Get[Either[io.circe.Error, AlgorithmPolicy]] = Get[String].map(AlgorithmPolicySerializer.decodeJson)
    implicit val algorithmPolicyTypePut: Put[AlgorithmPolicy] = Put[String].contramap(AlgorithmPolicySerializer.encodeJson)


  def insertQuery(project: Project) =
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
      ${project.problem},
      ${project.policy},
      ${project.featureType.toString},
      ${project.labelType.toString}
    )""".update
    
  def insert(project: Project) = insertQuery(project: Project).run.transact(xa)

  def readQuery(projectId: String) =
    sql"""
      SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      WHERE id=${projectId}
      """
        .query[(String, String, Either[io.circe.Error,ProblemType], Either[io.circe.Error, AlgorithmPolicy], String, String)]

  def read(projectId: String) = readQuery(projectId).unique.transact(xa)

  def readAll() =
    sql"""
        SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      """
      .query[(String, String, Either[io.circe.Error,ProblemType], Either[io.circe.Error, AlgorithmPolicy], String, String)]

}
