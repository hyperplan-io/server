package com.hyperplan.domain.repositories

import doobie._
import doobie.implicits._

import cats.implicits._
import cats.effect.IO

import com.hyperplan.domain.models.Algorithm
import com.hyperplan.domain.models.SecurityConfiguration
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.domain.errors.AlgorithmError._
import com.hyperplan.domain.repositories.AlgorithmsRepository.AlgorithmData
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization._

import doobie.postgres.sqlstate

class AlgorithmsRepository(implicit xa: Transactor[IO]) extends IOLogging {

  implicit val backendGet: Get[Either[io.circe.Error, Backend]] =
    Get[String].map(BackendSerializer.decodeJson)
  implicit val backendPut: Put[Backend] =
    Put[String].contramap(BackendSerializer.encodeJsonNoSpaces)

  implicit val securityConfigurationGet
      : Get[Either[io.circe.Error, SecurityConfiguration]] =
    Get[String].map(SecurityConfigurationSerializer.decodeJson)
  implicit val securityConfigurationPut: Put[SecurityConfiguration] =
    Put[String].contramap(SecurityConfigurationSerializer.encodeJsonNoSpaces)

  def insertQuery(algorithm: Algorithm): doobie.Update0 =
    sql"""INSERT INTO algorithms(
      id, 
      backend, 
      project_id,
      security
    ) VALUES(
      ${algorithm.id},
      ${algorithm.backend},
      ${algorithm.projectId},
      ${algorithm.security}
    )""".update

  def insert(algorithm: Algorithm): IO[Either[AlgorithmError, Algorithm]] =
    insertQuery(algorithm).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          AlgorithmAlreadyExistsError(
            AlgorithmAlreadyExistsError.message(algorithm.id)
          ): AlgorithmError
      }
      .transact(xa) *> IO.pure(algorithm.asRight)

  def readQuery(algorithmId: String): doobie.Query0[AlgorithmData] =
    sql"""
      SELECT id, backend, project_id, security
      FROM algorithms 
      WHERE id=$algorithmId
      """
      .query[AlgorithmData]

  def read(algorithmId: String): IO[Option[Algorithm]] =
    readQuery(algorithmId).option.transact(xa).flatMap {
      case Some(data) => dataToAlgorithm(data).map(_.some)
      case None => IO.pure(none[Algorithm])
    }

  def readForProjectQuery(projectId: String): doobie.Query0[AlgorithmData] =
    sql"""
      SELECT id, backend, project_id, security
      FROM algorithms 
      WHERE project_id=$projectId
      """
      .query[AlgorithmData]

  def readForProject(projectId: String): IO[List[Algorithm]] =
    readForProjectQuery(projectId)
      .to[List]
      .transact(xa)
      .flatMap(dataListToAlgorithm)

  def readAllQuery(): doobie.Query0[AlgorithmData] =
    sql"""
      SELECT id, backend, project_id, security
      FROM algorithms 
      """
      .query[AlgorithmData]

  def readAll(): IO[List[Algorithm]] =
    readAllQuery().to[List].transact(xa).flatMap(dataListToAlgorithm)

  def dataToAlgorithm(data: AlgorithmData) = data match {
    case (
        id,
        Right(backend),
        projectId,
        Right(security)
        ) =>
      IO.pure(
        Algorithm(id, backend, projectId, security)
      )

    case algorithmData =>
      logger.warn(
        s"Could not rebuild algorithm with repository, data is $algorithmData"
      ) *> IO.raiseError(
        AlgorithmDataIsIncorrectError(
          AlgorithmDataIsIncorrectError.message(
            data._1
          )
        )
      )
  }

  def dataListToAlgorithm(dataList: List[AlgorithmData]) =
    (dataList.map(dataToAlgorithm)).sequence

}

object AlgorithmsRepository {
  type AlgorithmData = (
      String,
      Either[io.circe.Error, Backend],
      String,
      Either[io.circe.Error, SecurityConfiguration]
  )
}
