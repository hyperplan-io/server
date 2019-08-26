/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

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
      : Get[Either[io.circe.Error, FeatureVectorDescriptor]] =
    Get[String].map(FeaturesConfigurationSerializer.decodeJson)
  implicit val featuresConfigurationPut: Put[FeatureVectorDescriptor] =
    Put[String].contramap(FeaturesConfigurationSerializer.encodeJsonNoSpaces)

  implicit val featureConfigurationGet
      : Get[Either[io.circe.Error, FeatureDescriptor]] =
    Get[String].map(
      FeaturesConfigurationSerializer.decodeFeatureConfigurationJson
    )
  implicit val featureConfigurationPut: Put[FeatureDescriptor] =
    Put[String].contramap(
      FeaturesConfigurationSerializer.encodeJsonConfigurationNoSpaces
    )

  implicit val featureconfigurationlistget
      : Get[Either[io.circe.Error, List[FeatureDescriptor]]] =
    Get[String].map(
      FeaturesConfigurationSerializer.decodeFeatureConfigurationListJson
    )

  implicit val featureconfigurationlistput: Put[List[FeatureDescriptor]] =
    Put[String].contramap(
      FeaturesConfigurationSerializer.encodeJsonConfigurationListNoSpaces
    )

  implicit val labelsConfigurationGet
      : Get[Either[io.circe.Error, LabelVectorDescriptor]] =
    Get[String].map(LabelsConfigurationSerializer.decodeJson)
  implicit val labelsConfigurationPut: Put[LabelVectorDescriptor] =
    Put[String].contramap(LabelsConfigurationSerializer.encodeJsonNoSpaces)

  implicit val labelConfigurationGet
      : Get[Either[io.circe.Error, LabelDescriptor]] =
    Get[String].map(LabelsConfigurationSerializer.decodeLabelConfigurationJson)
  implicit val labelConfigurationPut: Put[LabelDescriptor] =
    Put[String].contramap(
      LabelsConfigurationSerializer.encodeJsonConfigurationNoSpaces
    )
  implicit val labelConfigurationListGet
      : Get[Either[io.circe.Error, List[LabelDescriptor]]] =
    Get[String].map(
      LabelsConfigurationSerializer.decodeLabelConfigurationListJson
    )
  implicit val featureConfigurationListPut: Put[List[LabelDescriptor]] =
    Put[String].contramap(
      LabelsConfigurationSerializer.encodeJsonConfigurationListNoSpaces
    )

  def transact[T](io: ConnectionIO[T]) =
    io.transact(xa)

  def insertFeaturesQuery(features: FeatureVectorDescriptor): doobie.Update0 =
    sql"""INSERT INTO features(
      id, 
      data,
      created_at
    ) VALUES(
      ${features.id},
      ${features.data},
      NOW()
    )""".update

  def insertFeatures(
      features: FeatureVectorDescriptor
  ): IO[Either[FeatureVectorDescriptorError, Int]] =
    insertFeaturesQuery(features).run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          FeatureVectorDescriptorAlreadyExistError(features.id)
      }
      .transact(xa)

  def insertLabelsQuery(labels: LabelVectorDescriptor): doobie.Update0 =
    sql"""INSERT INTO labels(
      id, 
      data,
      created_at
    ) VALUES(
      ${labels.id},
      ${labels.data},
      NOW()
    )""".update

  def insertLabels(labels: LabelVectorDescriptor) =
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

  def readFeatures(id: String): ConnectionIO[Option[FeatureVectorDescriptor]] =
    readFeaturesQuery(id).option
      .flatMap {
        case Some(data) => dataToFeaturesConfiguration(data).map(_.some)
        case None => none[FeatureVectorDescriptor].pure[ConnectionIO]
      }

  def deleteLabelsQuery(labelsId: String): doobie.Update0 =
    sql"""DELETE FROM labels WHERE id = $labelsId""".update

  def deleteLabels(labelsId: String): IO[Int] =
    deleteLabelsQuery(labelsId).run
      .transact(xa)

  def deleteFeaturesQuery(featuresId: String): doobie.Update0 =
    sql"""DELETE FROM features WHERE id = $featuresId""".update

  def deleteFeatures(featuresId: String): IO[Int] =
    deleteFeaturesQuery(featuresId).run
      .transact(xa)

  def readLabelsQuery(
      id: String
  ): doobie.Query0[DomainRepository.LabelsConfigurationData] =
    sql"""
      SELECT id, data 
      FROM labels 
      WHERE id=$id
      """
      .query[DomainRepository.LabelsConfigurationData]

  def readLabels(id: String): ConnectionIO[Option[LabelVectorDescriptor]] =
    readLabelsQuery(id).option.flatMap {
      case Some(data) => dataToLabelsConfiguration(data).map(_.some)
      case None => none[LabelVectorDescriptor].pure[ConnectionIO]
    }

  def readAllFeaturesQuery()
      : doobie.Query0[DomainRepository.FeaturesConfigurationData] =
    sql"""
      SELECT id, data 
      FROM features
      """
      .query[DomainRepository.FeaturesConfigurationData]

  def readAllFeatures(): ConnectionIO[List[FeatureVectorDescriptor]] =
    readAllFeaturesQuery()
      .to[List]
      .flatMap(_.map(dataToFeaturesConfiguration _).sequence)

  def readAllLabelsQuery()
      : doobie.Query0[DomainRepository.LabelsConfigurationData] =
    sql"""
      SELECT id, data 
      FROM labels
      """
      .query[DomainRepository.LabelsConfigurationData]

  def readAllLabels(): ConnectionIO[List[LabelVectorDescriptor]] =
    readAllLabelsQuery()
      .to[List]
      .flatMap(_.map(dataToLabelsConfiguration _).sequence)

  def dataToFeaturesConfiguration(
      data: DomainRepository.FeaturesConfigurationData
  ): ConnectionIO[FeatureVectorDescriptor] = data match {
    case (
        id,
        Right(data)
        ) =>
      FeatureVectorDescriptor(id, data).pure[ConnectionIO]

    case _ =>
      AsyncConnectionIO.raiseError(DomainClassDataIncorrect(data._1))
  }

  def dataToLabelsConfiguration(
      data: DomainRepository.LabelsConfigurationData
  ): ConnectionIO[LabelVectorDescriptor] = data match {
    case (
        id,
        Right(data)
        ) =>
      LabelVectorDescriptor(id, data).pure[ConnectionIO]

    case labelsClassData =>
      AsyncConnectionIO.raiseError(DomainClassDataIncorrect(data._1))
  }

  def dataListToFeaturesConfiguration(
      dataList: List[DomainRepository.FeaturesConfigurationData]
  ) =
    (dataList.map(dataToFeaturesConfiguration)).sequence
}

object DomainRepository {
  type FeaturesConfigurationData =
    (String, Either[io.circe.Error, List[FeatureDescriptor]])
  type LabelsConfigurationData =
    (String, Either[io.circe.Error, LabelDescriptor])
}
