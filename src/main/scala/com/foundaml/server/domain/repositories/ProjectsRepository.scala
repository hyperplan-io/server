package com.foundaml.server.domain.repositories

import doobie._
import doobie.implicits._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.domain.models._
import com.foundaml.server.infrastructure.serialization._

class ProjectsRepository(implicit xa: Transactor[Task]) {

  implicit val problemTypeGet: Get[Either[io.circe.Error, ProblemType]] =
    Get[String].map(ProblemTypeSerializer.decodeJson)
  implicit val problemTypePut: Put[ProblemType] =
    Put[String].contramap(ProblemTypeSerializer.encodeJson)

  implicit val algorithmPolicyTypeGet
      : Get[Either[io.circe.Error, AlgorithmPolicy]] =
    Get[String].map(AlgorithmPolicySerializer.decodeJson)
  implicit val algorithmPolicyTypePut: Put[AlgorithmPolicy] =
    Put[String].contramap(AlgorithmPolicySerializer.encodeJson)

  def insertQuery(project: Project) =
    sql"""INSERT INTO projects(
      id, 
      name, 
      algorithm_policy,
      problem,
      features_class,
      features_size,
      labels_class,
      labels_size
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.policy},
      ${project.configuration.problem},
      ${project.configuration.featureClass},
      ${project.configuration.featuresSize},
      ${project.configuration.labelsClass},
      ${project.configuration.labelsSize}
    )""".update

  def insert(project: Project) = insertQuery(project: Project).run.transact(xa)

  def readQuery(projectId: String) =
    sql"""
      SELECT id, name, algorithm_policy, problem, features_class, features_size, labels_class, labels_size
      FROM projects
      WHERE id=$projectId
      """
      .query[
        (
            String,
            String,
            Either[io.circe.Error, AlgorithmPolicy],
            Either[io.circe.Error, ProblemType],
            String,
            Int,
            String,
            Int
        )
      ]

  def read(projectId: String) = readQuery(projectId).unique.transact(xa)

  def readAll() =
    sql"""
        SELECT id, name, algorithm_policy, problem, features_class, features_size, labels_class, labels_size
      FROM projects
      """
      .query[
        (
            String,
            String,
            Either[io.circe.Error, AlgorithmPolicy],
            Either[io.circe.Error, ProblemType],
            String,
            Int,
            String,
            Int
        )
      ]

}
