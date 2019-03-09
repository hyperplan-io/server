package com.foundaml.server.services.infrastructure.http

import cats.effect.Effect
import io.circe.Json
import org.http4s.circe.CirceEntityCodec._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import scalaz.zio.Task
import scalaz.zio.interop.catz._
import cats.effect._
import com.foundaml.server.services.infrastructure.http.requests._

//import com.foundaml.server.services.serialization._

class PredictionsHttpService extends Http4sDsl[Task] {

  //implicit val decoder = jsonOf[F, PredictionRequest]

  val service: HttpService[Task] = {
    HttpService[Task] {
      case req @ POST -> Root =>
        for {
        request <- req.as[PredictionRequest]
        resp <- Ok(Json.obj("message" -> Json.fromString(""))) 
        } yield resp
    }
  }
}
