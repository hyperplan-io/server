package com.foundaml.server.application.controllers

import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.domain.models.errors.LabelNotFound
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.infrastructure.serialization.LabelSerializer
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scalaz.zio.Task
import scalaz.zio.interop.catz._

class ExamplesController(
    predictionsService: PredictionsService
) extends Http4sDsl[Task] {

  object LabelIdMatcher extends QueryParamDecoderMatcher[String]("labelId")
  object PredictionIdMatcher
      extends QueryParamDecoderMatcher[String]("predictionId")

  val service: HttpService[Task] = {
    HttpService[Task] {
      case POST -> Root :? PredictionIdMatcher(predictionId) +& LabelIdMatcher(
            labelId
          ) =>
        (for {
          example <- predictionsService.addExample(predictionId, labelId)
        } yield example).flatMap { example =>
          Ok(LabelSerializer.encodeJson(example))
        }.catchAll {
          case LabelNotFound(_) =>
            NotFound(s"The label $labelId does not exist")
        }
    }
  }
}
