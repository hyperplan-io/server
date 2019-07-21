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
import scala.collection.immutable.Nil
import com.hyperplan.domain.models.features.ReferenceFeatureType

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
  ): ValidationResult[Unit] =
    (existingFeatures match {
      case None => Validated.valid(())
      case Some(value) => Validated.invalid(FeaturesAlreadyExistError(value.id))
    }).toValidatedNec

  def validateReferenceFeaturesExist(
      references: List[(String, Option[FeaturesConfiguration])]
  ): Validated[NonEmptyChain[FeaturesError], Unit] = {
    val emptyReferences = references.collect {
      case (featureId, None) =>
        featureId
    }
    emptyReferences match {
      case head :: tl =>
        Validated.invalid[NonEmptyChain[FeaturesError], Unit](
          NonEmptyChain(
            ReferenceFeatureDoesNotExistError(head),
            tl.map(id => ReferenceFeatureDoesNotExistError(id)): _*
          )
        )
      case Nil =>
        Validated.valid[NonEmptyChain[FeaturesError], Unit](())
    }
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

  def validateUniqueness(names: List[String]): ValidationResult[Unit] =
    Either
      .cond[FeaturesError, Unit](
        names.distinct.length == names.length,
        Unit,
        DuplicateFeatureIds()
      )
      .toValidatedNec

  def validate(
      featuresConfiguration: FeaturesConfiguration,
      existingFeatures: Option[FeaturesConfiguration],
      references: List[(String, Option[FeaturesConfiguration])],
      names: List[String]
  ): ValidationResult[Unit] =
    (
      validateFeaturesDoesNotAlreadyExist(existingFeatures),
      validateReferenceFeaturesExist(references),
      validateReferenceFeaturesDimension(featuresConfiguration),
      validateUniqueness(names)
    ).mapN {
      case (features, references, _, _) => Unit
    }

  def createFeatures(
      features: FeaturesConfiguration
  ): EitherT[IO, NonEmptyChain[FeaturesError], FeaturesConfiguration] =
    for {
      existingFeatures <- EitherT.liftF(readFeatures(features.id))
      featuresNames = features.data.map(_.name)
      referenceFeaturesIO = features.data.collect {
        case FeatureConfiguration(
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
        validate(features, existingFeatures, referenceFeatures, featuresNames).toEither
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
