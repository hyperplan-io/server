package com.foundaml.server.infrastructure.serialization

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import scalaz.zio.Task
import scalaz.zio.interop.catz._
import com.foundaml.server.application.controllers.requests.{
  PostProjectConfiguration,
  PostProjectRequest
}
import com.foundaml.server.domain.models.{FeaturesConfiguration, ProblemType}
import io.circe.Decoder

object PostProjectRequestEntitySerializer {

  import io.circe.generic.semiauto._

  implicit val problemTypeDecoder: Decoder[ProblemType] =
    ProblemTypeSerializer.decoder
  implicit val postProjectConfigurationDecoder
      : Decoder[PostProjectConfiguration] = deriveDecoder
  implicit val featuresConfigurationDecoder: Decoder[FeaturesConfiguration] =
    FeaturesConfigurationSerializer.decoder
  implicit val postProjectRequestDecoder: Decoder[PostProjectRequest] =
    deriveDecoder

  implicit val entityDecoder: EntityDecoder[Task, PostProjectRequest] =
    jsonOf[Task, PostProjectRequest]

}
