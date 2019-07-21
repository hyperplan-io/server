package com.hyperplan.test.application

import cats.effect.IO

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import org.scalatest.FlatSpec

import org.scalatest.Matchers
import org.scalatest._

import com.hyperplan.test.TestDatabase
import com.hyperplan.application.controllers.LabelsController

import com.hyperplan.domain.errors._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.labels._

import com.hyperplan.domain.services._
import com.hyperplan.domain.repositories._

import com.hyperplan.infrastructure.serialization.errors.ErrorsSerializer
import com.hyperplan.infrastructure.serialization._

class LabelsControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  override def beforeAll(): Unit = initSchema()
  override def beforeEach(): Unit = wipeTables()

  implicit val labelsDecoderList =
    LabelsConfigurationSerializer.entityDecoderList
  implicit val labelsDecoder = LabelsConfigurationSerializer.entityDecoder
  implicit val labelsErrorDecoder = ErrorsSerializer.labelErrorEntityDecoder
  implicit val labelsConfigurationEncoder =
    LabelsConfigurationSerializer.entityEncoder

  val domainRepository = new DomainRepository()(xa)
  val domainService = new DomainService(
    domainRepository
  )

  val labelsController = new LabelsController(
    domainService
  )
  it should "fetch an empty labels list" in {
    val response = labelsController.service
      .run(
        Request(
          method = Method.GET,
          uri = uri"/"
        )
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[LabelsConfiguration]](
        response,
        Status.Ok,
        Some(Nil)
      )
    )
  }

  it should "not return a label that does not exist" in {
    val response = labelsController.service
      .run(
        Request(
          method = Method.GET,
          uri = uri"/my-id"
        )
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[LabelsError]](
        response,
        Status.NotFound,
        Some(
          List(
            LabelsDoesNotExist(
              "The labels my-id does not exist"
            )
          )
        )
      )
    )
  }

  it should "create labels of type oneOf" in {

    val entityBody = LabelsConfiguration(
      id = "test",
      data = OneOfLabelsConfiguration(
        oneOf = Set("label-1", "label-2"),
        description = "my description"
      )
    )

    val response = labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[LabelsConfiguration](
        response,
        Status.Created,
        Some(
          entityBody
        )
      )
    )
  }

  it should "create labels of type dynamic" in {

    val entityBody = LabelsConfiguration(
      id = "test",
      data = DynamicLabelsConfiguration(
        description = "my description"
      )
    )

    val response = labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[LabelsConfiguration](
        response,
        Status.Created,
        Some(
          entityBody
        )
      )
    )
  }

  it should "eliminate duplicate label ids" in {

    val entityBody = LabelsConfiguration(
      id = "test",
      data = OneOfLabelsConfiguration(
        oneOf = Set("feature-1", "feature-1", "feature-2"),
        description = "my description"
      )
    )

    val response = labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[LabelsConfiguration](
        response,
        Status.Created,
        Some(
          entityBody
        )
      )
    )
  }

  it should "fail to create a label that already exists" in {

    val entityBody = LabelsConfiguration(
      id = "test",
      data = OneOfLabelsConfiguration(
        oneOf = Set("feature-1", "feature-2"),
        description = "my description"
      )
    )

    val response = labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)

    response
      .map { previousResponse =>
        println(previousResponse.status)
        val entityBody2 = LabelsConfiguration(
          id = "test",
          data = OneOfLabelsConfiguration(
            oneOf = Set("feature-1", "feature-2"),
            description = "my description"
          )
        )
        val response2 = labelsController.service
          .run(
            Request[IO](
              method = Method.POST,
              uri = uri"/"
            ).withEntity(entityBody2)
          )
          .value
          .map(_.get)

        assert(
          ControllerTestUtils.check[List[LabelsError]](
            response2,
            Status.BadRequest,
            Some(
              List(
                LabelsAlreadyExist(
                  s"The labels test already exist"
                )
              )
            )
          )
        )
      }
      .unsafeRunSync()

  }

}
