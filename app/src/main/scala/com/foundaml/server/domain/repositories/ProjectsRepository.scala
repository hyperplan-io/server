package com.foundaml.server.domain.repositories

import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.implicits._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.ProjectAlreadyExists
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.domain.models.backends.Backend
import doobie.postgres.sqlstate
import com.foundaml.server.domain.models.errors.AlgorithmDataIncorrect

import com.foundaml.server.domain.models._

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
    Put[String].contramap(FeaturesConfigurationSerializer.encodeJsonNoSpaces)

  implicit val projectConfigurationGet
      : Get[Either[io.circe.Error, ProjectConfiguration]] =
    Get[String].map(ProjectConfigurationSerializer.decodeJson)

  implicit val projectConfigurationPut: Put[ProjectConfiguration] =
    Put[String].contramap(ProjectConfigurationSerializer.encodeJsonString)

  implicit val backendGet: Get[Either[io.circe.Error, Backend]] =
    Get[String].map(BackendSerializer.decodeJson)
  implicit val backendPut: Put[Backend] =
    Put[String].contramap(BackendSerializer.encodeJsonNoSpaces)

  implicit val securityConfigurationGet
      : Get[Either[io.circe.Error, SecurityConfiguration]] =
    Get[String].map(SecurityConfigurationSerializer.decodeJson)
  implicit val securityConfigurationPut: Put[SecurityConfiguration] =
    Put[String].contramap(SecurityConfigurationSerializer.encodeJsonNoSpaces)

  import ProjectsRepository._

  val separator = ";"
  implicit val labelsTypeGet: Get[Set[String]] =
    Get[String].map(_.split(separator).toSet)
  implicit val labelsTypePut: Put[Set[String]] =
    Put[String].contramap(labels => s"${labels.mkString(separator)}")

  def transact[T](io: ConnectionIO[T]) =
    io.transact(xa)

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

  def read(projectId: String): ConnectionIO[Project] = {
    readQuery(projectId).unique.flatMap(dataToProject)
    readQuery(projectId).unique
      .flatMap(dataToProject)
      .flatMap(retrieveProjectAlgorithms)
  }

  def readAllProjectsQuery =
    sql"""
        SELECT projects.id, name, problem, algorithm_policy, configuration
      FROM projects 
      """
      .query[ProjectsRepository.ProjectData]

  def readProjectAlgorithmsQuery(
      projectId: String
  ): doobie.Query0[AlgorithmData] =
    sql"""
      SELECT id, backend, project_id, security
      FROM algorithms 
      WHERE project_id=$projectId
      """
      .query[AlgorithmData]

  def readProjectAlgorithms(projectId: String): ConnectionIO[List[Algorithm]] =
    readProjectAlgorithmsQuery(projectId)
      .to[List]
      .flatMap(dataListToAlgorithm)

  def readAllAlgorithmsQuery(): doobie.Query0[AlgorithmData] =
    sql"""
      SELECT id, backend, project_id, security
      FROM algorithms 
      """
      .query[AlgorithmData]

  def readAll =
    readAllProjectsQuery
      .to[List]
      .flatMap(dataListToProject)
      .flatMap(retrieveProjectsAlgorithms)

  def updateQuery(project: Project) =
    sql"""
        UPDATE projects SET name=${project.name}, algorithm_policy = ${project.policy}
      """.update

  def update(project: Project) = updateQuery(project).run

  def retrieveProjectAlgorithms(project: Project): ConnectionIO[Project] =
    (project match {
      case project: ClassificationProject =>
        readProjectAlgorithms(project.id).map { newAlgorithms =>
          project.copy(
            algorithms = newAlgorithms
          )
        }
      case project: RegressionProject =>
        readProjectAlgorithms(project.id).map { newAlgorithms =>
          project.copy(
            algorithms = newAlgorithms
          )
        }
    })

  def retrieveProjectsAlgorithms(projects: List[Project]) =
    projects.map(retrieveProjectAlgorithms).sequence

}

object ProjectsRepository {
  type ProjectData = (
      String,
      String,
      Either[io.circe.Error, ProblemType],
      Either[io.circe.Error, AlgorithmPolicy],
      Either[io.circe.Error, ProjectConfiguration]
  )

  type AlgorithmData = (
      String,
      Either[io.circe.Error, Backend],
      String,
      Either[io.circe.Error, SecurityConfiguration]
  )

  type ProjectEntityData = (
      String,
      String,
      Either[io.circe.Error, ProblemType],
      Either[io.circe.Error, AlgorithmPolicy],
      Either[io.circe.Error, ProjectConfiguration],
      String,
      Either[io.circe.Error, Backend],
      String,
      Either[io.circe.Error, SecurityConfiguration]
  )

  import com.foundaml.server.domain.models.errors.ProjectDataInconsistent
  def dataToProject(data: ProjectData): ConnectionIO[Project] = data match {
    case (
        id,
        name,
        Right(Classification),
        Right(policy),
        Right(projectConfiguration: ClassificationConfiguration)
        ) =>
      (ClassificationProject(
        id,
        name,
        projectConfiguration,
        Nil,
        policy
      ): Project).pure[ConnectionIO]
    case (
        id,
        name,
        Right(Regression),
        Right(policy),
        Right(projectConfiguration: RegressionConfiguration)
        ) =>
      (RegressionProject(
        id,
        name,
        projectConfiguration,
        Nil,
        policy
      ): Project).pure[ConnectionIO]
    case projectData =>
      AsyncConnectionIO.raiseError(ProjectDataInconsistent(data._1))
  }

  def dataListToProject(dataList: List[ProjectData]) =
    (dataList.map(dataToProject)).sequence

  def dataToAlgorithm(data: AlgorithmData): ConnectionIO[Algorithm] =
    data match {
      case (
          id,
          Right(backend),
          projectId,
          Right(security)
          ) =>
        Algorithm(id, backend, projectId, security).pure[ConnectionIO]
      case algorithmData =>
        println(algorithmData)
        AsyncConnectionIO.raiseError(AlgorithmDataIncorrect(data._1))
    }

  def dataListToAlgorithm(
      dataList: List[AlgorithmData]
  ): ConnectionIO[List[Algorithm]] =
    (dataList.map(dataToAlgorithm)).sequence

}
