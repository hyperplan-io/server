package com.hyperplan.domain.validators

import cats.data._
import cats.implicits._

import scala.collection.immutable.Nil

import com.hyperplan.domain.errors._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._


object DomainValidator {

  type FeaturesValidationResult[A] =
    ValidatedNec[FeatureVectorDescriptorError, A]
  type LabelsValidationResult[A] = ValidatedNec[LabelVectorDescriptorError, A]


  def validateLabelsDoesNotAlreadyExist(
                                         existingLabels: Option[LabelVectorDescriptor]
                                       ): LabelsValidationResult[Unit] =
    (existingLabels match {
      case None => Validated.valid(())
      case Some(value) =>
        Validated.invalid(LabelVectorDescriptorAlreadyExist(value.id))
    }).toValidatedNec

  def validateLabelsNotEmpty(labelsConfiguration: LabelVectorDescriptor) =
    labelsConfiguration.data match {
      case DynamicLabelsDescriptor(description) => Validated.valid(Unit)
      case OneOfLabelsDescriptor(oneOf, description) =>
        Either
          .cond(oneOf.nonEmpty, Unit, OneOfLabelVectorDescriptorCannotBeEmpty())
          .toValidatedNec
    }

  def validateFeaturesDoesNotAlreadyExist(
                                           existingFeatures: Option[FeatureVectorDescriptor]
                                         ): FeaturesValidationResult[Unit] =
    (existingFeatures match {
      case None => Validated.valid(())
      case Some(value) =>
        Validated.invalid(FeatureVectorDescriptorAlreadyExistError(value.id))
    }).toValidatedNec

  def validateReferenceFeaturesExist(
                                      references: List[(String, Option[FeatureVectorDescriptor])]
                                    ): Validated[NonEmptyChain[FeatureVectorDescriptorError], Unit] = {
    val emptyReferences = references.collect {
      case (featureId, None) =>
        featureId
    }
    emptyReferences match {
      case head :: tl =>
        Validated.invalid[NonEmptyChain[FeatureVectorDescriptorError], Unit](
          NonEmptyChain(
            ReferenceFeatureDoesNotExistError(head),
            tl.map(id => ReferenceFeatureDoesNotExistError(id)): _*
          )
        )
      case Nil =>
        Validated.valid[NonEmptyChain[FeatureVectorDescriptorError], Unit](())
    }
  }
  def validateReferenceFeaturesDimension(
                                          featuresConfiguration: FeatureVectorDescriptor
                                        ): Validated[NonEmptyChain[FeatureVectorDescriptorError], Unit] = {
    val dimensionErrors: List[FeatureVectorDescriptorError] =
      featuresConfiguration.data.collect {
        case featureConfiguration: FeatureDescriptor
          if featureConfiguration.isReference && featureConfiguration.dimension != Scalar =>
          UnsupportedDimensionError(
            featureConfiguration.name,
            featureConfiguration.dimension
          )
      }
    dimensionErrors match {
      case firstError :: errors =>
        Validated.invalid[NonEmptyChain[FeatureVectorDescriptorError], Unit](
          NonEmptyChain(firstError, errors: _*)
        )
      case Nil =>
        Validated.valid[NonEmptyChain[FeatureVectorDescriptorError], Unit](Unit)
    }
  }

  def validateUniqueness(names: List[String]): FeaturesValidationResult[Unit] =
    Either
      .cond[FeatureVectorDescriptorError, Unit](
      names.distinct.length == names.length,
      Unit,
      DuplicateFeatureIds()
    )
      .toValidatedNec

  def validateFeatures(
                        featuresConfiguration: FeatureVectorDescriptor,
                        existingFeatures: Option[FeatureVectorDescriptor],
                        references: List[(String, Option[FeatureVectorDescriptor])],
                        names: List[String]
                      ): FeaturesValidationResult[Unit] =
    (
      validateFeaturesDoesNotAlreadyExist(existingFeatures),
      validateReferenceFeaturesExist(references),
      validateReferenceFeaturesDimension(featuresConfiguration),
      validateUniqueness(names)
    ).mapN {
      case (features, references, _, _) => Unit
    }

  def validateLabels(
                      existingLabels: Option[LabelVectorDescriptor],
                      labelsConfiguration: LabelVectorDescriptor
                    ): LabelsValidationResult[Unit] =
    (
      validateLabelsDoesNotAlreadyExist(existingLabels),
      validateLabelsNotEmpty(labelsConfiguration)
    ).mapN {
      case (_, _) => Unit
    }

}
