package com.foundaml.server.domain.repositories

import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.implicits._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.ProjectAlreadyExists
import com.foundaml.server.infrastructure.serialization._
import doobie.postgres.sqlstate

class ProjectsRepository(implicit xa: Transactor[IO]) {

  implicit val problemTypeGet: Get[Either[io.circe.Error, ProblemType]] =
    Get[String].map(ProblemTypeSerializer.decodeJson)
  implicit val problemTypePut: Put[ProblemType] =
    Put[String].contramap(ProblemTypeSerializer.encodeJsonString)

  implicit val algorithmPolicyTypeGet
      : Get[Either[io.circe.Error, AlgorithmPolicy]] =
    Get[String].map(AlgorithmPolicySerializer.decodeJson)
  implicit val algorithmPolicyTypePut: Put[AlgorithmPolicy] =
    Put[String].contramap(AlgorithmPolicySerializer.encodeJsonString)

  implicit val featuresConfigurationGet
      : Get[Either[io.circe.Error, FeaturesConfiguration]] =
    Get[String].map(FeaturesConfigurationSerializer.decodeJson)

  implicit val featuresConfigurationPut: Put[FeaturesConfiguration] =
    Put[String].contramap(FeaturesConfigurationSerializer.encodeJson)

  implicit val projectConfigurationGet
      : Get[Either[io.circe.Error, ProjectConfiguration]] =
    Get[String].map(ProjectConfigurationSerializer.decodeJson)

  implicit val projectConfigurationPut: Put[ProjectConfiguration] =
    Put[String].contramap(ProjectConfigurationSerializer.encodeJsonString)

  val separator = ";"
  implicit val labelsTypeGet: Get[Set[String]] =
    Get[String].map(_.split(separator).toSet)
  implicit val labelsTypePut: Put[Set[String]] =
    Put[String].contramap(labels => s"${labels.mkString(separator)}")

  def insertQuery(project: Project) =
    sql"""INSERT INTO projects(
      id, 
      name,
      problem,
      algorithm_policy,
      configuration
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.problem},
      ${project.policy},
      ${project.configuration}
    )""".update

  def insert(project: Project) =
    insertQuery(project: Project).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          ProjectAlreadyExists(project.id)
      }
      .transact(xa)

  def readQuery(projectId: String) =
    sql"""
      SELECT id, name, problem, algorithm_policy, configuration
      FROM projects
      WHERE id=$projectId
      """
      .query[ProjectsRepository.ProjectData]

  def read(projectId: String) = readQuery(projectId).unique.transact(xa)

  def readAll() =
    sql"""
        SELECT id, name, problem, algorithm_policy, configuration
      FROM projects
      """
      .query[ProjectsRepository.ProjectData]

}

object ProjectsRepository {
  type ProjectData = (
      String,
      String,
      Either[io.circe.Error, ProblemType],
      Either[io.circe.Error, AlgorithmPolicy],
      Either[io.circe.Error, ProjectConfiguration]
  )
}
