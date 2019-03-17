package com.foundaml.server.infrastructure.serialization

import com.foundaml.server.application.controllers.requests.PostAlgorithmRequest
import com.foundaml.server.domain.models.backends.Backend
import io.circe.Decoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import scalaz.zio.Task
import scalaz.zio.interop.catz._

object PostAlgorithmRequestEntitySerializer {

  import io.circe.generic.semiauto._

  implicit val backendDecoder: Decoder[Backend] = BackendSerializer.Implicits.decoder

  implicit val postAlgorithmDecoder: Decoder[PostAlgorithmRequest] = deriveDecoder

  implicit val entityDecoder: EntityDecoder[Task, PostAlgorithmRequest] =
    jsonOf[Task, PostAlgorithmRequest]

}
