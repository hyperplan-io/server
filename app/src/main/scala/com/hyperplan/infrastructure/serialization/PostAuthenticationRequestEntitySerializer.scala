package com.hyperplan.infrastructure.serialization

import com.foundaml.server.controllers.requests.PostProjectRequest

import io.circe._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import cats.effect.IO
import cats.implicits._

import com.foundaml.server.controllers.requests.PostAuthenticationRequest

object PostAuthenticationRequestEntitySerializer {

  implicit val decoder: Decoder[PostAuthenticationRequest] =
    (c: HCursor) =>
      for {
        username <- c.downField("username").as[String]
        password <- c.downField("password").as[String]
      } yield PostAuthenticationRequest(username, password)

  implicit val entityDecoder: EntityDecoder[IO, PostAuthenticationRequest] =
    jsonOf[IO, PostAuthenticationRequest]

}
