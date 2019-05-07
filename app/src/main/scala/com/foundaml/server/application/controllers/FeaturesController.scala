package com.foundaml.server.application.controllers

import cats.Functor
import org.http4s.{HttpRoutes, HttpService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import cats.implicits._
import com.foundaml.server.application.controllers.requests._
import com.foundaml.server.domain.models.errors.{
  AlgorithmAlreadyExists,
  IncompatibleFeatures,
  IncompatibleLabels
}
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.services.AlgorithmsService
import com.foundaml.server.infrastructure.serialization._
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.domain.services.DomainService

class FeaturesController(domainService: DomainService)
    extends Http4sDsl[IO]
    with IOLogging {

  import cats.MonadError
  val service: HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        (for {
          domainClass <- req.as[FeaturesConfiguration](
            MonadError[IO, Throwable],
            FeaturesConfigurationSerializer.entityDecoder
          )
          _ <- domainService.createFeatures(domainClass)
          _ <- logger.info(
            s"Domain class created with id ${domainClass.id}"
          )

        } yield domainClass).flatMap { domainClass =>
          Ok()
        }
      case req @ GET -> Root =>
        NotImplemented()
    }
  }

}
