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

  val separator = ","
  implicit val labelsTypeGet
  : Get[Set[String]] =
    Get[String].map(_.split(separator).toSet)
  implicit val labelsTypePut: Put[Set[String]] =
    Put[String].contramap(_.mkString(separator))

  def insertQuery(project: Project) =
    sql"""INSERT INTO projects(
      id, 
      name, 
      algorithm_policy,
      problem,
      features_class,
      features_size,
      labels,
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.policy},
      ${project.configuration.problem},
      ${project.configuration.featureClass},
      ${project.configuration.featuresSize},
      ${project.configuration.labels},
    )""".update

  def insert(project: Project) = insertQuery(project: Project).run.transact(xa)

  def readQuery(projectId: String) =
    sql"""
      SELECT id, name, algorithm_policy, problem, features_class, features_size, labels
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
            Set[String]
        )
      ]

  def read(projectId: String) = readQuery(projectId).unique.transact(xa)

  def readAll() =
    sql"""
        SELECT id, name, algorithm_policy, problem, features_class, features_size, labels
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
            Set[String]
        )
      ]

}
