package com.hyperplan.test.application

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import org.scalatest.FlatSpec

import com.hyperplan.test.TestDatabase

import com.hyperplan.application.controllers.FeaturesController
import com.hyperplan.infrastructure.serialization._

import com.hyperplan.domain.services._
import com.hyperplan.domain.repositories._
import org.scalatest.Matchers
import com.hyperplan.domain.models.FeaturesConfiguration
import org.scalatest._
import com.hyperplan.domain.errors.FeaturesError
import com.hyperplan.infrastructure.serialization.errors.ErrorsSerializer
import com.hyperplan.domain.errors.FeaturesDoesNotExistError

class FeaturesControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  override def beforeAll(): Unit = initSchema()
  override def beforeEach(): Unit = wipeTables()

  implicit val featuresDecoderList =
    FeaturesConfigurationSerializer.entityDecoderList
  implicit val featuresDecoder = FeaturesConfigurationSerializer.entityDecoder
  implicit val featuresErrorDecoder = ErrorsSerializer.featureErrorEntityDecoder

  val domainRepository = new DomainRepository()(xa)
  val domainService = new DomainService(
    domainRepository
  )

  val featuresController = new FeaturesController(
    domainService
  )

  it should "fetch an empty features list" in {
    val response = featuresController.service
      .run(
        Request(
          method = Method.GET,
          uri = uri"/"
        )
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[FeaturesConfiguration]](
        response,
        Status.Ok,
        Some(Nil)
      )
    )
  }

  it should "not return a feature that does not exist" in {
    val response = featuresController.service
      .run(
        Request(
          method = Method.GET,
          uri = uri"/my-id"
        )
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[FeaturesError]](
        response,
        Status.NotFound,
        Some(
          List(
            FeaturesDoesNotExistError(
              "The features my-id does not exist"
            )
          )
        )
      )
    )
  }

}
