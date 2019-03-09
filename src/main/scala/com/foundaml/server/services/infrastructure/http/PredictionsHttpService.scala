package com.foundaml.server.services.infrastructure.http

import io.circe.Json
import org.http4s.HttpService
import org.http4s.dsl.Http4sDsl

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.services.infrastructure.http.requests._
import com.foundaml.server.services.serialization.CirceEncoders._

import com.foundaml.server.services.domain.PredictionsService

import com.foundaml.server.services.domain._
import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._
import com.foundaml.server.models.backends._
import com.foundaml.server.models._


class PredictionsHttpService(predictionsService: PredictionsService) extends Http4sDsl[Task] {

  implicit val requestDecoder: EntityDecoder[Task, PredictionRequest] = jsonOf[Task, PredictionRequest]
  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>


          Ok(for {
          predictionRequest <- req.as[PredictionRequest]
          prediction <- predict(predictionRequest)
          _ = println(prediction)
        } yield Json.fromString(prediction.toString)) 
    }
  }


  def predict(request: PredictionRequest) ={
    def compute(features: TensorFlowClassificationFeatures): TensorFlowClassificationLabels =
      TensorFlowClassificationLabels(
        List(
          TensorFlowClassicationLabel(
            List(1, 2, 3),
            List(0.0f, 0.1f, 0.2f),
            List("class1", "class2", "class3"),
            List(0.0f, 0.0f, 0.0f)
          )
        )
       ) 
        
       val defaultAlgorithm = Algorithm[TensorFlowClassificationFeatures, TensorFlowClassificationLabels](
        "algorithm id",
        Local(compute)
        )
      
        val project = Project[
          TensorFlowClassificationFeatures,
          TensorFlowClassificationLabels
        ](
          "id",
          "example project",
          Classification,
          Map.empty,
          DefaultAlgorithm(defaultAlgorithm) 
        )

    request.features match {
      case f: TensorFlowClassificationFeatures =>
        predictionsService.predict(
          f,
          project,
          request.algorithmId
        )
      case _ =>
        Task.fail(new IllegalStateException("Unhandled features"))
    }
  }

}
