package com.hyperplan.domain.services

import cats.data.{EitherT, NonEmptyChain}
import cats.effect.IO

import com.hyperplan.domain.errors.{
  FeatureVectorDescriptorError,
  LabelVectorDescriptorError
}
import com.hyperplan.domain.models.{
  FeatureVectorDescriptor,
  LabelVectorDescriptor
}

trait DomainService {
  def readAllFeatures: IO[List[FeatureVectorDescriptor]]
  def readFeatures(id: String): IO[Option[FeatureVectorDescriptor]]
  def createFeatures(
      features: FeatureVectorDescriptor
  ): EitherT[IO, NonEmptyChain[FeatureVectorDescriptorError], FeatureVectorDescriptor]

  def readAllLabels: IO[List[LabelVectorDescriptor]]
  def readLabels(id: String): IO[Option[LabelVectorDescriptor]]
  def createLabels(
      labelsConfiguration: LabelVectorDescriptor
  ): EitherT[IO, NonEmptyChain[LabelVectorDescriptorError], LabelVectorDescriptor]
}
