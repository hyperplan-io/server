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

package com.hyperplan.domain.services

import cats.effect.IO
import cats.implicits._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.repositories.DomainRepository
import com.hyperplan.domain.models._
import com.hyperplan.domain.errors._
import doobie.util.invariant.UnexpectedEnd
import cats.data._
import cats.data.Validated._
import com.hyperplan.domain.models.features.ReferenceFeature
import com.hyperplan.domain.models.features.Scalar

import scala.collection.immutable.Nil
import com.hyperplan.domain.models.features.ReferenceFeatureType
import com.hyperplan.domain.validators.DomainValidator

class DomainServiceLive(domainRepository: DomainRepository)
    extends DomainService
    with IOLogging {

  type FeaturesValidationResult[A] =
    ValidatedNec[FeatureVectorDescriptorError, A]
  type LabelsValidationResult[A] = ValidatedNec[LabelVectorDescriptorError, A]

  def readAllFeatures =
    domainRepository.transact(domainRepository.readAllFeatures())

  def readFeatures(id: String): IO[Option[FeatureVectorDescriptor]] =
    domainRepository.transact(domainRepository.readFeatures(id))
  def deleteFeatures(id: String): IO[Int] = domainRepository.deleteFeatures(id)

  def readAllLabels =
    domainRepository.transact(domainRepository.readAllLabels())

  def readLabels(id: String): IO[Option[LabelVectorDescriptor]] =
    domainRepository.transact(domainRepository.readLabels(id))

  def deleteLabels(id: String): IO[Int] = domainRepository.deleteLabels(id)

  def createFeatures(
      features: FeatureVectorDescriptor
  ): EitherT[IO, NonEmptyChain[FeatureVectorDescriptorError], FeatureVectorDescriptor] =
    for {
      existingFeatures <- EitherT.liftF(readFeatures(features.id))
      featuresNames = features.data.map(_.name)
      referenceFeaturesIO = features.data.collect {
        case FeatureDescriptor(
            name,
            ReferenceFeatureType(reference),
            _,
            _
            ) =>
          readFeatures(reference).map { config =>
            name -> config
          }
      }
      referenceFeatures <- EitherT.liftF(referenceFeaturesIO.sequence)
      validated <- EitherT.fromEither[IO](
        DomainValidator
          .validateFeatures(
            features,
            existingFeatures,
            referenceFeatures,
            featuresNames
          )
          .toEither
      )
      _ <- EitherT.liftF(
        domainRepository.insertFeatures(features)
      )
    } yield features

  def createLabels(
      labelsConfiguration: LabelVectorDescriptor
  ): EitherT[IO, NonEmptyChain[LabelVectorDescriptorError], LabelVectorDescriptor] =
    for {
      existingLabels <- EitherT.liftF(readLabels(labelsConfiguration.id))
      _ <- EitherT.fromEither[IO](
        DomainValidator
          .validateLabels(existingLabels, labelsConfiguration)
          .toEither
      )
      _ <- EitherT.liftF(
        domainRepository
          .insertLabels(labelsConfiguration)
      )
    } yield labelsConfiguration

}
