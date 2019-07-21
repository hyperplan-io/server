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
import com.hyperplan.domain.models.features.One

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
  def validateReferenceFeaturesDimension(
      featuresConfiguration: FeaturesConfiguration
  ): Validated[NonEmptyChain[FeaturesError], Unit] = {
    val dimensionErrors: List[FeaturesError] =
      featuresConfiguration.data.collect {
        case featureConfiguration: FeatureConfiguration
            if featureConfiguration.isReference && featureConfiguration.dimension != One =>
          UnsupportedDimensionError(
            featureConfiguration.name,
            featureConfiguration.dimension
          )
      }
    dimensionErrors match {
      case firstError :: errors =>
        Validated.invalid[NonEmptyChain[FeaturesError], Unit](
          NonEmptyChain(firstError, errors: _*)
        )
      case Nil =>
        Validated.valid[NonEmptyChain[FeaturesError], Unit](Unit)
    }
  }

  def validate(
      featuresConfiguration: FeaturesConfiguration,
      existingFeatures: Option[FeaturesConfiguration],
      references: List[Option[FeaturesConfiguration]]
  ): ValidationResult[List[FeaturesConfiguration]] =
    (
      validateFeaturesDoesNotAlreadyExist(existingFeatures),
      validateReferenceFeaturesExist(references),
      validateReferenceFeaturesDimension(featuresConfiguration)
    ).mapN {
      case (features, references, _) => references
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
        validate(features, existingFeatures, referenceFeatures).toEither
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
