package com.hyperplan.infrastructure.serialization

import io.circe.Decoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import cats.effect.IO
import cats.implicits._
import com.hyperplan.application.controllers.requests.PredictionRequest
import com.hyperplan.domain.models.features.Features.Features
import com.hyperplan.infrastructure.serialization.features.FeaturesSerializer

object PredictionRequestEntitySerializer {

  import io.circe._, io.circe.generic.semiauto._

  implicit val featuresDecoder: Decoder[Features] =
    FeaturesSerializer.Implicits.decoder

  implicit val decoder: Decoder[PredictionRequest] =
    PredictionRequestSerializer.decoder

  val requestDecoder: EntityDecoder[IO, PredictionRequest] =
    jsonOf[IO, PredictionRequest]

}
