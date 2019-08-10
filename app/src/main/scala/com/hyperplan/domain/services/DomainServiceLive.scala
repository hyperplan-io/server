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
    domainRepository.readAllFeatures

  def readFeatures(id: String): IO[Option[FeatureVectorDescriptor]] =
    domainRepository.readFeatures(id).map(_.some).handleErrorWith {
      case UnexpectedEnd =>
        IO.pure(none[FeatureVectorDescriptor])
    }

  def readAllLabels = domainRepository.readAllLabels

  def readLabels(id: String): IO[Option[LabelVectorDescriptor]] =
    domainRepository.readLabels(id).map(_.some).handleErrorWith {
      case UnexpectedEnd =>
        IO.pure(none[LabelVectorDescriptor])
    }

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
