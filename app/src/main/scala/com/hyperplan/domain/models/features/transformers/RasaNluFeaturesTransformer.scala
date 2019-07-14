package com.hyperplan.domain.models.features.transformers

import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.domain.models.features._

case class RasaNluFeaturesTransformer(
    fields: Map[String, String]
) {
  def transformer(
      features: Features,
      fields: Map[String, String] = fields
  ): Either[Throwable, RasaNluFeatures] = ???
}
