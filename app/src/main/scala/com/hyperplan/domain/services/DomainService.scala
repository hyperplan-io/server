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

class DomainService(domainRepository: DomainRepository) extends IOLogging {

  type ValidationResult[A] = ValidatedNec[FeaturesError, A]

  def readAllFeatures =
    domainRepository.readAllFeatures

  def readFeatures(id: String): IO[Option[FeaturesConfiguration]] =
    domainRepository.readFeatures(id).map(_.some).handleErrorWith {
      case UnexpectedEnd =>
        IO.pure(none[FeaturesConfiguration])
    }

  def readAllLabels = domainRepository.readAllLabels
  def readLabels(id: String) =
    domainRepository.readLabels(id).handleErrorWith {
      case UnexpectedEnd =>
        IO.raiseError(LabelsClassDoesNotExist(id))
    }

  def validateFeaturesDoesNotAlreadyExist(
      existingFeatures: Option[FeaturesConfiguration]
  ): ValidationResult[String] =
    Either
      .cond(existingFeatures.isEmpty, "", FeaturesAlreadyExistError(""))
      .toValidatedNec

  def validateReferenceFeaturesExist(
      references: List[Option[FeaturesConfiguration]]
  ): ValidationResult[List[FeaturesConfiguration]] = {
    val nonEmptyReferences = references.collect {
      case Some(featuresConfiguration) =>
        featuresConfiguration
    }
    Either
      .cond(
        nonEmptyReferences.length == references.length,
        nonEmptyReferences,
        ReferenceFeatureDoesNotExistError()
      )
      .toValidatedNec
  }

  def validate(
      existingFeatures: Option[FeaturesConfiguration],
      references: List[Option[FeaturesConfiguration]]
  ): ValidationResult[List[FeaturesConfiguration]] =
    (
      validateFeaturesDoesNotAlreadyExist(existingFeatures),
      validateReferenceFeaturesExist(references)
    ).mapN {
      case (features, references) => references
    }

  def createFeatures(
      features: FeaturesConfiguration
  ): EitherT[IO, NonEmptyChain[FeaturesError], FeaturesConfiguration] =
    for {
      existingFeatures <- EitherT.liftF(readFeatures(features.id))
      referenceFeaturesIO = features.data.collect {
        case featuresConfiguration if featuresConfiguration.isReference =>
          readFeatures(featuresConfiguration.name)
      }
      referenceFeatures <- EitherT.liftF(referenceFeaturesIO.sequence)
      validated <- EitherT.fromEither[IO](
        validate(existingFeatures, referenceFeatures).toEither
      )
      _ <- EitherT.liftF(
        domainRepository.insertFeatures(features)
      )
    } yield features

  def createLabels(labels: LabelsConfiguration) =
    domainRepository
      .insertLabels(labels)
      .flatMap(_.fold(err => IO.raiseError(err), result => IO.pure(result)))

}
