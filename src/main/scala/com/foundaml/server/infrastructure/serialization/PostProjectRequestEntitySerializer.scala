package com.foundaml.server.infrastructure.serialization

import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.application.controllers.requests.PostProjectRequest

object PostProjectRequestEntitySerializer {
  implicit val discriminator: Configuration = CirceEncoders.discriminator
  implicit val entityDecoder: EntityDecoder[Task, PostProjectRequest] =
    jsonOf[Task, PostProjectRequest]

}
