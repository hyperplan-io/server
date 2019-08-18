package com.hyperplan.domain.repositories

import cats.data.{NonEmptyChain, NonEmptyList}
import doobie._
import doobie.implicits._
import doobie.postgres._
import cats.effect.IO
import cats.implicits._
import com.hyperplan.domain.errors.ProjectError
import com.hyperplan.domain.errors.ProjectError.ProjectAlreadyExistsError
import com.hyperplan.domain.models
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.domain.models.backends.Backend
import com.hyperplan.domain.models._
import com.hyperplan.infrastructure.logging.IOLogging
import eu.timepit.refined.collection.NonEmpty

class ProjectsRepository(domainRepository: DomainRepository)(
    implicit xa: Transactor[IO]
) extends IOLogging {

  /*
   * project
   */
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
      : Get[Either[io.circe.Error, FeatureVectorDescriptor]] =
    Get[String].map(FeaturesConfigurationSerializer.decodeJson)

  implicit val featuresConfigurationPut: Put[FeatureVectorDescriptor] =
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

  implicit val readProjectRow: Read[ProjectRowData] = Read[
    (
        String,
        String,
        String,
        String,
        String,
        Option[String],
        Option[String],
        String,
        String,
        String
    )
  ].map {
    case (
        projectId,
        projectName,
        problem,
        policy,
        featuresId,
        labelsId,
        topic,
        algorithmId,
        backend,
        securityConfiguration
        ) =>
      (
        projectId,
        projectName,
        ProblemTypeSerializer.decodeJson(problem),
        AlgorithmPolicySerializer.decodeJson(policy),
        featuresId,
        labelsId,
        topic,
        algorithmId,
        BackendSerializer.decodeJson(backend),
        SecurityConfigurationSerializer.decodeJson(securityConfiguration)
      )
  }

  implicit val writes: Write[Algorithm] = Write[
    (
        String,
        Backend,
        String,
        SecurityConfiguration
    )
  ].contramap(
    algorithm =>
      (algorithm.id, algorithm.backend, algorithm.projectId, algorithm.security)
  )

  def transact[T](io: ConnectionIO[T]) =
    io.transact(xa)

  def insertProjectQuery(project: Project) =
    project.configuration match {
      case ClassificationConfiguration(features, labels, dataStream) =>
        sql"""INSERT INTO projects(
          id,
          name,
          problem,
          algorithm_policy,
          features_id,
          labels_id,
          topic,
          created_at
        ) VALUES(
          ${project.id},
          ${project.name},
          ${project.problem},
          ${project.policy},
          ${features.id},
          ${labels.id},
          ${dataStream.map(_.topic).getOrElse("")},
          NOW()
        )""".update

      case RegressionConfiguration(features, dataStream) =>
        sql"""INSERT INTO projects(
          id,
          name,
          problem,
          algorithm_policy,
          features_id,
          topic,
          created_at
        ) VALUES(
          ${project.id},
          ${project.name},
          ${project.problem},
          ${project.policy},
          ${features.id},
          ${dataStream.map(_.topic).getOrElse("")},
          NOW()
        )""".update
    }

  def insertProject(
      project: Project
  ): ConnectionIO[Either[NonEmptyChain[ProjectError], Project]] =
    insertProjectQuery(project: Project).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          NonEmptyChain(
            ProjectAlreadyExistsError(
              ProjectAlreadyExistsError.message(project.id)
            ): ProjectError
          ).asLeft[Project]
      }
      .flatMap {
        case Right(_) =>
          insertManyAlgorithm(project.algorithms).map(_ => project.asRight)
        case Left(err) =>
          err.pure[ConnectionIO]
      }

  def readProjectQuery(projectId: String): Query0[ProjectRowData] =
    sql"""
      SELECT projects.id, name, problem, algorithm_policy, features_id, labels_id, topic, algorithms.id, backend, security
      FROM projects
      JOIN algorithms on algorithms.project_id = projects.id
      WHERE projects.id=$projectId
      """
      .query[ProjectsRepository.ProjectRowData]

  def rowToProject(
      projectRowData: ProjectRowData,
      allRows: List[ProjectRowData]
  ) = {
    val (
      projectId,
      projectName,
      projectProblem,
      projectPolicy,
      featuresId,
      maybeLabelsId,
      topic,
      _,
      _,
      _
    ) = projectRowData

    projectPolicy match {
      case Left(_) =>
        none[Project].pure[ConnectionIO]
      case Right(policy) =>
        val algorithms = allRows.collect {
          case (
              id,
              _,
              _,
              _,
              _,
              _,
              _,
              algorithmId,
              Right(backend),
              Right(securityConfiguration)
              ) if id == projectId =>
            Algorithm(
              algorithmId,
              backend,
              projectId,
              securityConfiguration
            )
        }
        val streamConfiguration = topic.map(t => StreamConfiguration(t))

        val projectConfigurationConnectionIO =
          projectProblem.fold[ConnectionIO[Option[ProjectConfiguration]]](
            _ => none[ProjectConfiguration].pure[ConnectionIO], {
              case Classification =>
                maybeLabelsId match {
                  case Some(labelsId) =>
                    (
                      domainRepository.readFeatures(featuresId),
                      domainRepository.readLabels(labelsId)
                    ).mapN {
                      case (Some(features), Some(labels)) =>
                        ClassificationConfiguration(
                          features,
                          labels,
                          streamConfiguration
                        ).some
                      case (None, Some(labels)) =>
                        none[ProjectConfiguration]
                      case (Some(features), None) =>
                        none[ProjectConfiguration]
                      case (None, None) =>
                        none[ProjectConfiguration]
                    }
                  case None =>
                    none[ProjectConfiguration].pure[ConnectionIO]
                }

              case Regression =>
                domainRepository.readFeatures(featuresId).map {
                  case Some(features) =>
                    RegressionConfiguration(
                      features,
                      streamConfiguration
                    ).some
                  case None =>
                    none[ProjectConfiguration]
                }
            }
          )
        projectConfigurationConnectionIO.map {
          case Some(projectConfiguration: ClassificationConfiguration) =>
            ClassificationProject(
              projectId,
              projectName,
              projectConfiguration,
              algorithms,
              policy
            ).some
          case Some(projectConfiguration: RegressionConfiguration) =>
            RegressionProject(
              projectId,
              projectName,
              projectConfiguration,
              algorithms,
              policy
            ).some
          case None =>
            none[Project]
        }
    }
  }

  def readProject(projectId: String): ConnectionIO[Option[Project]] = {

    readProjectQuery(projectId).to[List].flatMap {
      case allRows @ head :: _ =>
        rowToProject(head, allRows)
      case _ =>
        none[Project].pure[ConnectionIO]
    }

  }

  def deleteProjectQuery(
      projectId: String
  ): doobie.Update0 =
    sql"""DELETE FROM projects WHERE id = $projectId""".update

  def deleteProject(projectId: String): ConnectionIO[Int] =
    deleteProjectQuery(projectId).run

  def deleteProjectAlgorithmsQuery(
      projectId: String
  ): doobie.Update0 =
    sql"""DELETE FROM algorithms WHERE project_id = $projectId""".update

  def deleteProjectAlgorithms(projectId: String): ConnectionIO[Int] =
    deleteProjectAlgorithmsQuery(projectId).run

  def deleteAlgorithmQuery(
      projectId: String,
      algorithmId: String
  ): doobie.Update0 =
    sql"""DELETE FROM algorithms WHERE project_id = $projectId AND id = $algorithmId""".update

  def deleteAlgorithm(projectId: String, algorithmId: String): IO[Int] =
    deleteAlgorithmQuery(projectId, algorithmId).run
      .transact(xa)

  def readAllProjectsQuery: Query0[ProjectRowData] =
    sql"""
      SELECT projects.id, name, problem, algorithm_policy, features_id, labels_id, topic, algorithms.id, backend, security
      FROM projects
      INNER JOIN algorithms ON algorithms.project_id = projects.id
      """
      .query[ProjectsRepository.ProjectRowData]

  def readAllProjects: ConnectionIO[List[Project]] = {

    readAllProjectsQuery
      .to[List]
      .flatMap { rows =>
        rows
          .map { row =>
            rowToProject(row, rows)
          }
          .sequence
          .map(_.flatten)
      }
  }

  def updateProjectQuery(project: Project) =
    sql"""
        UPDATE projects SET name=${project.name}, algorithm_policy = ${project.policy}
      """.update

  def updateProject(project: Project) = updateProjectQuery(project).run

  def insertAlgorithmQuery(algorithm: Algorithm): doobie.Update0 =
    sql"""INSERT INTO algorithms(
      id,
      backend,
      project_id,
      security,
      created_at
    ) VALUES(
      ${algorithm.id},
      ${algorithm.backend},
      ${algorithm.projectId},
      ${algorithm.security},
      NOW()
    )""".update

  def insertAlgorithm(algorithm: Algorithm): ConnectionIO[Algorithm] =
    insertAlgorithmQuery(algorithm).run
      .flatMap(_ => algorithm.pure[ConnectionIO])

  def insertManyAlgorithm(algorithms: List[Algorithm]): ConnectionIO[Int] = {
    val sql = """INSERT INTO algorithms(
      id,
      backend,
      project_id,
      security,
      created_at
    ) VALUES(
      ?,?,?,?, NOW()
    )"""
    Update[Algorithm](sql).updateMany(algorithms)
  }

}

object ProjectsRepository {
  type ProjectRowData = (
      // project
      String, // project id
      String, // project name
      Either[io.circe.Error, ProblemType],
      Either[io.circe.Error, AlgorithmPolicy],
      String, // featuresId
      Option[String], // labelsId
      Option[String], // topic
      // algorithm
      String, // algorithm id
      Either[io.circe.Error, Backend],
      Either[io.circe.Error, SecurityConfiguration]
  )
}
