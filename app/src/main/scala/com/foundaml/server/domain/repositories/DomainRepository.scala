package com.foundaml.server.domain.repositories

import doobie._
import doobie.implicits._

import cats.implicits._
import cats.effect.IO

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.errors._

import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.serialization._

import doobie.postgres.sqlstate

class DomainRepository(implicit xa: Transactor[IO]) extends IOLogging {

  implicit val featuresConfigurationGet
      : Get[Either[io.circe.Error, FeaturesConfiguration]] =
    Get[String].map(FeaturesConfigurationSerializer.decodeJson)
  implicit val featuresConfigurationPut: Put[FeaturesConfiguration] =
    Put[String].contramap(FeaturesConfigurationSerializer.encodeJsonNoSpaces)

  def insertQuery(domainClass: DomainClass): doobie.Update0 =
    sql"""INSERT INTO domain(
      id, 
      data
    ) VALUES(
      ${domainClass.id},
      ${domainClass.data}
    )""".update

  def insert(domainClass: DomainClass) =
    insertQuery(domainClass).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          DomainClassAlreadyExists(domainClass.id)
      }
      .transact(xa)

  def readAllQuery(): doobie.Query0[DomainRepository.DomainClassData] =
    sql"""
      SELECT id, data 
      FROM domain 
      """
      .query[DomainRepository.DomainClassData]

  def readAll(): IO[List[DomainClass]] =
    readAllQuery().to[List].transact(xa).flatMap(dataListToAlgorithm)

  def dataToAlgorithm(data: DomainRepository.DomainClassData) = data match {
    case (
        id,
        Right(data)
        ) =>
      IO.pure(
        DomainClass(id, data)
      )

    case domainClassData =>
      logger.warn(
        s"Could not rebuild domain class with repository, data is $domainClassData"
      ) *> IO.raiseError(DomainClassDataIncorrect(data._1))
  }

  def dataListToAlgorithm(dataList: List[DomainRepository.DomainClassData]) =
    (dataList.map(dataToAlgorithm)).sequence
}

object DomainRepository {
  type DomainClassData = (String, Either[io.circe.Error, FeaturesConfiguration])
}
