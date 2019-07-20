package com.hyperplan.domain.repositories

import doobie._
import doobie.implicits._

import cats.implicits._
import cats.effect.IO

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.labels.Labels
import com.hyperplan.domain.errors._

import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.serialization._

import doobie.postgres.sqlstate

class DomainRepository(implicit xa: Transactor[IO]) extends IOLogging {

  implicit val featuresConfigurationGet
      : Get[Either[io.circe.Error, FeaturesConfiguration]] =
    Get[String].map(FeaturesConfigurationSerializer.decodeJson)
  implicit val featuresConfigurationPut: Put[FeaturesConfiguration] =
    Put[String].contramap(FeaturesConfigurationSerializer.encodeJsonNoSpaces)

  implicit val featureConfigurationGet
      : Get[Either[io.circe.Error, FeatureConfiguration]] =
    Get[String].map(
      FeaturesConfigurationSerializer.decodeFeatureConfigurationJson
    )
  implicit val featureConfigurationPut: Put[FeatureConfiguration] =
    Put[String].contramap(
      FeaturesConfigurationSerializer.encodeJsonConfigurationNoSpaces
    )

  implicit val featureconfigurationlistget
      : Get[Either[io.circe.Error, List[FeatureConfiguration]]] =
    Get[String].map(
      FeaturesConfigurationSerializer.decodeFeatureConfigurationListJson
    )

  implicit val featureconfigurationlistput: Put[List[FeatureConfiguration]] =
    Put[String].contramap(
      FeaturesConfigurationSerializer.encodeJsonConfigurationListNoSpaces
    )

  implicit val labelsConfigurationGet
      : Get[Either[io.circe.Error, LabelsConfiguration]] =
    Get[String].map(LabelsConfigurationSerializer.decodeJson)
  implicit val labelsConfigurationPut: Put[LabelsConfiguration] =
    Put[String].contramap(LabelsConfigurationSerializer.encodeJsonNoSpaces)

  implicit val labelConfigurationGet
      : Get[Either[io.circe.Error, LabelConfiguration]] =
    Get[String].map(LabelsConfigurationSerializer.decodeLabelConfigurationJson)
  implicit val labelConfigurationPut: Put[LabelConfiguration] =
    Put[String].contramap(
      LabelsConfigurationSerializer.encodeJsonConfigurationNoSpaces
    )
  implicit val labelConfigurationListGet
      : Get[Either[io.circe.Error, List[LabelConfiguration]]] =
    Get[String].map(
      LabelsConfigurationSerializer.decodeLabelConfigurationListJson
    )
  implicit val featureConfigurationListPut: Put[List[LabelConfiguration]] =
    Put[String].contramap(
      LabelsConfigurationSerializer.encodeJsonConfigurationListNoSpaces
    )

  def insertFeaturesQuery(features: FeaturesConfiguration): doobie.Update0 =
    sql"""INSERT INTO features(
      id, 
      data
    ) VALUES(
      ${features.id},
      ${features.data}
    )""".update

  def insertFeatures(features: FeaturesConfiguration) =
    insertFeaturesQuery(features).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          DomainClassAlreadyExists(features.id)
      }
      .transact(xa)

  def insertLabelsQuery(labels: LabelsConfiguration): doobie.Update0 =
    sql"""INSERT INTO labels(
      id, 
      data
    ) VALUES(
      ${labels.id},
      ${labels.data}
    )""".update

  def insertLabels(labels: LabelsConfiguration) =
    insertLabelsQuery(labels).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          DomainClassAlreadyExists(labels.id)
      }
      .transact(xa)

  def readFeaturesQuery(
      id: String
  ): doobie.Query0[DomainRepository.FeaturesConfigurationData] =
    sql"""
      SELECT id, data 
      FROM features 
      WHERE id=$id
      """
      .query[DomainRepository.FeaturesConfigurationData]

  def readFeatures(id: String): IO[FeaturesConfiguration] =
    readFeaturesQuery(id).unique
      .transact(xa)
      .flatMap(dataToFeaturesConfiguration _)

  def readLabelsQuery(
      id: String
  ): doobie.Query0[DomainRepository.LabelsConfigurationData] =
    sql"""
      SELECT id, data 
      FROM labels 
      WHERE id=$id
      """
      .query[DomainRepository.LabelsConfigurationData]

  def readLabels(id: String): IO[LabelsConfiguration] =
    readLabelsQuery(id).unique.transact(xa).flatMap(dataToLabelsConfiguration _)

  def readAllFeaturesQuery()
      : doobie.Query0[DomainRepository.FeaturesConfigurationData] =
    sql"""
      SELECT id, data 
      FROM features
      """
      .query[DomainRepository.FeaturesConfigurationData]

  def readAllFeatures(): IO[List[FeaturesConfiguration]] =
    readAllFeaturesQuery()
      .to[List]
      .transact(xa)
      .flatMap(_.map(dataToFeaturesConfiguration _).sequence)

  def readAllLabelsQuery()
      : doobie.Query0[DomainRepository.LabelsConfigurationData] =
    sql"""
      SELECT id, data 
      FROM labels
      """
      .query[DomainRepository.LabelsConfigurationData]

  def readAllLabels(): IO[List[LabelsConfiguration]] =
    readAllLabelsQuery()
      .to[List]
      .transact(xa)
      .flatMap(_.map(dataToLabelsConfiguration _).sequence)

  def dataToFeaturesConfiguration(
      data: DomainRepository.FeaturesConfigurationData
  ): IO[FeaturesConfiguration] = data match {
    case (
        id,
        Right(data)
        ) =>
      IO.pure(
        FeaturesConfiguration(id, data)
      )

    case featuresClassData =>
      logger.warn(
        s"Could not rebuild features class with repository, data is $featuresClassData"
      ) *> IO.raiseError(DomainClassDataIncorrect(data._1))
  }

  def dataToLabelsConfiguration(
      data: DomainRepository.LabelsConfigurationData
  ): IO[LabelsConfiguration] = data match {
    case (
        id,
        Right(data)
        ) =>
      IO.pure(
        LabelsConfiguration(id, data)
      )

    case labelsClassData =>
      logger.warn(
        s"Could not rebuild features class with repository, data is $labelsClassData"
      ) *> IO.raiseError(DomainClassDataIncorrect(data._1))
  }

  def dataListToFeaturesConfiguration(
      dataList: List[DomainRepository.FeaturesConfigurationData]
  ) =
    (dataList.map(dataToFeaturesConfiguration)).sequence
}

object DomainRepository {
  type FeaturesConfigurationData =
    (String, Either[io.circe.Error, List[FeatureConfiguration]])
  type LabelsConfigurationData =
    (String, Either[io.circe.Error, LabelConfiguration])
}
