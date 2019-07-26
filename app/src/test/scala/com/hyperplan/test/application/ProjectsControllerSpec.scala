package com.hyperplan.test.application

import cats.effect.IO

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import org.scalatest.FlatSpec
import scalacache._
import scalacache.caffeine._

import org.scalatest.Matchers
import org.scalatest._

import com.hyperplan.test.TestDatabase
import com.hyperplan.application.controllers.FeaturesController

import com.hyperplan.domain.errors._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._

import com.hyperplan.domain.services._
import com.hyperplan.domain.repositories._

import com.hyperplan.infrastructure.serialization.errors.ErrorsSerializer
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.application.controllers.ProjectsController

class ProjectsControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  override def beforeAll(): Unit = initSchema()
  override def beforeEach(): Unit = wipeTables()

  implicit val projectEntityDecoder: EntityDecoder[IO, Project] =
    ProjectSerializer.entityDecoder
  implicit val projectListEntityDecoder: EntityDecoder[IO, List[Project]] =
    ProjectSerializer.entityListDecoder
  implicit val projectEntityEncoder: EntityEncoder[IO, Project] =
    ProjectSerializer.entityEncoder
  implicit val projectListEntityEncoder: EntityEncoder[IO, List[Project]] =
    ProjectSerializer.entityListEncoder

  val projectRepository = new ProjectsRepository()(xa)
  val domainRepository = new DomainRepository()(xa)
  val domainService = new DomainService(domainRepository)

  val projectCache: Cache[Project] = CaffeineCache[Project]
  val projectsService = new ProjectsService(
    projectRepository,
    domainService,
    projectCache
  )

  val projectsController = new ProjectsController(
    projectsService
  )

  it should "fetch an empty projects list" in {
    val response = projectsController.service
      .run(
        Request(
          method = Method.GET,
          uri = uri"/"
        )
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[Project]](
        response,
        Status.Ok,
        Some(Nil)
      )
    )
  }

  /*
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

  it should "create a basic feature" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[FeaturesConfiguration](
        response,
        Status.Created,
        Some(
          entityBody
        )
      )
    )
  }

  it should "not create a feature that contains duplicate ids" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        ),
        FeatureConfiguration(
          name = "feature-1",
          featuresType = IntFeatureType,
          dimension = One,
          description = "my description"
        ),
        FeatureConfiguration(
          name = "feature-3",
          featuresType = FloatFeatureType,
          dimension = Vector,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[FeaturesError]](
        response,
        Status.BadRequest,
        Some(
          List(
            DuplicateFeatureIds()
          )
        )
      )
    )
  }

  it should "fail to create a feature that already exists" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
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
        val entityBody2 = FeaturesConfiguration(
          id = "test",
          data = List(
            FeatureConfiguration(
              name = "blabla",
              featuresType = ReferenceFeatureType("test"),
              dimension = One,
              description = "my description"
            )
          )
        )

        val response2 = featuresController.service
          .run(
            Request[IO](
              method = Method.POST,
              uri = uri"/"
            ).withEntity(entityBody2)
          )
          .value
          .map(_.get)

        assert(
          ControllerTestUtils.check[List[FeaturesError]](
            response2,
            Status.BadRequest,
            Some(
              List(
                FeaturesAlreadyExistError(
                  s"The feature test already exists"
                )
              )
            )
          )
        )
      }
      .unsafeRunSync()

  }

  it should "fail to create a feature that references a another that does not exist" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        ),
        FeatureConfiguration(
          name = "feature-2",
          featuresType = ReferenceFeatureType("this-features-does-not-exist"),
          dimension = One,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[FeaturesError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ReferenceFeatureDoesNotExistError(
              s"The feature feature-2 does not exist and cannot be referenced"
            )
          )
        )
      )
    )
  }

  it should "create features with a reference that exists" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
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
        val entityBody2 = FeaturesConfiguration(
          id = "test2",
          data = List(
            FeatureConfiguration(
              name = "blabla",
              featuresType = ReferenceFeatureType("test"),
              dimension = One,
              description = "my description"
            )
          )
        )

        val response2 = featuresController.service
          .run(
            Request[IO](
              method = Method.POST,
              uri = uri"/"
            ).withEntity(entityBody2)
          )
          .value
          .map(_.get)

        assert(
          ControllerTestUtils.check[FeaturesConfiguration](
            response2,
            Status.Created,
            Some(
              entityBody2
            )
          )
        )
      }
      .unsafeRunSync()
  }

  it should "not support reference features with dimensions Vector" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    response
      .map { _ =>
        val entityBody2 = FeaturesConfiguration(
          id = "test2",
          data = List(
            FeatureConfiguration(
              name = "feature-test",
              featuresType = ReferenceFeatureType("test"),
              dimension = Vector,
              description = "my description"
            )
          )
        )

        val response2 = featuresController.service
          .run(
            Request[IO](
              method = Method.POST,
              uri = uri"/"
            ).withEntity(entityBody2)
          )
          .value
          .map(_.get)

        assert(
          ControllerTestUtils.check[List[FeaturesError]](
            response2,
            Status.BadRequest,
            Some(
              List(
                UnsupportedDimensionError(
                  s"The feature feature-test cannot be used with dimension Vector"
                )
              )
            )
          )
        )
      }
      .unsafeRunSync()
  }

  it should "not support reference features with dimensions Matrix" in {

    val entityBody = FeaturesConfiguration(
      id = "test",
      data = List(
        FeatureConfiguration(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = One,
          description = "my description"
        )
      )
    )

    val response = featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    response
      .map { _ =>
        val entityBody2 = FeaturesConfiguration(
          id = "test2",
          data = List(
            FeatureConfiguration(
              name = "feature-test",
              featuresType = ReferenceFeatureType("test"),
              dimension = Matrix,
              description = "my description"
            )
          )
        )

        val response2 = featuresController.service
          .run(
            Request[IO](
              method = Method.POST,
              uri = uri"/"
            ).withEntity(entityBody2)
          )
          .value
          .map(_.get)

        assert(
          ControllerTestUtils.check[List[FeaturesError]](
            response2,
            Status.BadRequest,
            Some(
              List(
                UnsupportedDimensionError(
                  s"The feature feature-test cannot be used with dimension Matrix"
                )
              )
            )
          )
        )
      }
      .unsafeRunSync()
  }
 */

}
