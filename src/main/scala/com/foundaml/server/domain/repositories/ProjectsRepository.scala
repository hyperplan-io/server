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

  implicit val featuresConfigurationGet
      : Get[Either[io.circe.Error, FeaturesConfiguration]] =
    Get[String].map(FeaturesConfigurationSerializer.decodeJson)

  implicit val featuresConfigurationPut: Put[FeaturesConfiguration] =
    Put[String].contramap(FeaturesConfigurationSerializer.encodeJson)

  val separator = ";"
  implicit val labelsTypeGet: Get[Set[String]] =
    Get[String].map(_.split(separator).toSet)
  implicit val labelsTypePut: Put[Set[String]] =
    Put[String].contramap(labels => s"${labels.mkString(separator)}")

  def insertQuery(project: Project) =
    sql"""INSERT INTO projects(
      id, 
      name, 
      algorithm_policy,
      problem,
      features_configuration,
      labels
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.policy},
      ${project.configuration.problem},
      ${project.configuration.features},
      ${project.configuration.labels}
    )""".update

  def insert(project: Project) = insertQuery(project: Project).run.transact(xa)

  def readQuery(projectId: String) =
    sql"""
      SELECT id, name, algorithm_policy, problem, features_configuration, labels
      FROM projects
      WHERE id=$projectId
      """
      .query[ProjectsRepository.ProjectData]

  def read(projectId: String) = readQuery(projectId).unique.transact(xa)

  def readAll() =
    sql"""
        SELECT id, name, algorithm_policy, problem, features_configuration, labels
      FROM projects
      """
      .query[ProjectsRepository.ProjectData]

}

object ProjectsRepository {
  type ProjectData = (
      String,
      String,
      Either[io.circe.Error, AlgorithmPolicy],
      Either[io.circe.Error, ProblemType],
      Either[io.circe.Error, FeaturesConfiguration],
      Set[String]
  )
}
