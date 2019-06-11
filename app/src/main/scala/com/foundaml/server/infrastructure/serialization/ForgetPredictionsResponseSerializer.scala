package com.foundaml.server.infrastructure.serialization

import io.circe.syntax._
import io.circe._

import com.foundaml.server.application.controllers.responses._

object ForgetPredictionsResponseSerializer {

  implicit val encoder: Encoder[ForgetPredictionsResponse] =
    (response: ForgetPredictionsResponse) =>
      Json.obj(
        ("entityName", Json.fromString(response.entityName)),
        ("entityId", Json.fromString(response.entityId)),
        ("count", Json.fromInt(response.count))
      )

  def encodeJson(response: ForgetPredictionsResponse) =
    response.asJson

}
