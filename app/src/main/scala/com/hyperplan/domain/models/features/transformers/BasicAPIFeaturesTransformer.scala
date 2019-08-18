package com.hyperplan.domain.models.features.transformers

import cats.implicits._

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.errors._
import cats.data.NonEmptyList
import scala.collection.immutable.Nil

case class BasicHttpAPIFeaturesTransformer(
    ) {

  def transform(
      features: Features
  ): Features = features

}
