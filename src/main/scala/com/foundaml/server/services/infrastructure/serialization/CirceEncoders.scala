package com.foundaml.server.services.infrastructure.serialization

import io.circe._
import io.circe.generic.semiauto._

import com.foundaml.server.models._
import com.foundaml.server.services.infrastructure.http.requests._
import com.foundaml.server.models.features._

import cats.syntax.functor._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
import io.circe.syntax._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

object CirceEncoders {

  val discriminator: Configuration = Configuration.default.withDiscriminator("class")

  implicit val oneToManyDecoder: Decoder[OneToMany] =
    deriveDecoder[OneToMany]
  implicit val oneToManyEncoder: Encoder[OneToMany] =
    deriveEncoder[OneToMany]

  implicit val tfClassificationFeaturesDecoder
      : Decoder[TensorFlowClassificationFeatures] =
    deriveDecoder[TensorFlowClassificationFeatures]
  implicit val tfClassificationFeaturesEncoder
      : Encoder[TensorFlowClassificationFeatures] =
    deriveEncoder[TensorFlowClassificationFeatures]

  implicit val encodeEvent: Encoder[Features] = Encoder.instance {
    case foo @ TensorFlowClassificationFeatures(_, _) => foo.asJson
  }

  implicit val decodeEvent: Decoder[Features] =
    List[Decoder[Features]](
      Decoder[TensorFlowClassificationFeatures].widen
    ).reduceLeft(_ or _)

  implicit val predictionDecoder: Decoder[Prediction] =
    deriveDecoder[Prediction]
  implicit val predictionEncoder: Encoder[Prediction] =
    deriveEncoder[Prediction]

  implicit val predictionRequestDecoder: Decoder[PredictionRequest] =
    deriveDecoder[PredictionRequest]
  implicit val predictionRequestEncoder: Encoder[PredictionRequest] =
    deriveEncoder[PredictionRequest]

}
