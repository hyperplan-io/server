package com.hyperplan.test.application

import cats.effect.IO
import cats.implicits._

import org.http4s._

import com.hyperplan.application.controllers.requests.{
  PatchProjectRequest,
  PostProjectRequest
}
import com.hyperplan.application.controllers.{
  FeaturesController,
  LabelsController,
  ProjectsController
}
import com.hyperplan.domain.errors.ProjectError
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.models
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.repositories._
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.errors.{
  ErrorsSerializer,
  ProjectErrorsSerializer
}
import com.hyperplan.test.TestDatabase
import org.scalatest.{FlatSpec, Matchers, _}
import scalacache._
import scalacache.caffeine._
import com.hyperplan.application.controllers.AlgorithmsController
import com.hyperplan.domain.models.backends.LocalClassification
import com.hyperplan.application.controllers.requests.PostAlgorithmRequest
import com.hyperplan.test.ProjectUtils
import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.infrastructure.serialization.errors.AlgorithmErrorsSerializer
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.features.transformers.RasaNluFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.RasaNluLabelsTransformer

class AlgorithmsControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  override def beforeAll(): Unit = initSchema()
  override def beforeEach(): Unit = wipeTables()

  implicit val algorithmEntityEncoder: EntityEncoder[IO, Algorithm] =
    AlgorithmsSerializer.entityEncoder
  implicit val algorithmEntityDecoder: EntityDecoder[IO, Algorithm] =
    AlgorithmsSerializer.entityDecoder

  implicit val errorDecoder: EntityDecoder[IO, List[AlgorithmError]] =
    AlgorithmErrorsSerializer.algorithmErrorEntityDecoder

  implicit val requestEntityEncoder: EntityEncoder[IO, PostAlgorithmRequest] =
    PostAlgorithmRequestEntitySerializer.entityEncoder
  implicit val requestEntityDecoder: EntityDecoder[IO, PostAlgorithmRequest] =
    PostAlgorithmRequestEntitySerializer.entityDecoder

  val projectRepository = new ProjectsRepository()(xa)
  val domainRepository = new DomainRepository()(xa)
  val algorithmsRepository = new AlgorithmsRepository()(xa)

  val domainService = new DomainService(domainRepository)

  val projectCache: Cache[Project] = CaffeineCache[Project]
  val projectsService = new ProjectsService(
    projectRepository,
    domainService,
    projectCache
  )
  val algorithmsService = new AlgorithmsService(
    projectsService,
    algorithmsRepository,
    projectRepository
  )

  val featuresController = new FeaturesController(
    domainService
  )

  val labelsController = new LabelsController(
    domainService
  )

  val projectsController = new ProjectsController(
    projectsService
  )

  val algorithmsController = new AlgorithmsController(
    algorithmsService
  )

  it should "fail to create an algorithm for a project that does not exist" in {
    val id = "test"
    val backend = LocalClassification(Set.empty)
    val projectId = "myproject"
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      id,
      projectId,
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(id, backend, projectId, security)
    val response = algorithmsController.service
      .run(
        Request(
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityRequest)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[AlgorithmError]](
        response,
        Status.BadRequest,
        Some(
          List(
            AlgorithmError.ProjectDoesNotExistError(
              AlgorithmError.ProjectDoesNotExistError.message(projectId)
            )
          )
        )
      )
    )
  }

  it should "successfully create an algorithm" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor = ProjectUtils.createLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val id = "test"
    val projectId = project.id
    val backend = LocalClassification(Set.empty)
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      id,
      projectId,
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(id, backend, projectId, security)
    val response = algorithmsController.service
      .run(
        Request(
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityRequest)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[Algorithm](
        response,
        Status.Created,
        Some(
          expectedAlgorithm
        )
      )
    )
  }

  it should "fail to create an algorithm because the id is not alpha numerical" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor = ProjectUtils.createLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val id = "&erpokq"
    val projectId = project.id
    val backend = LocalClassification(Set.empty)
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      id,
      projectId,
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(id, backend, projectId, security)
    val response = algorithmsController.service
      .run(
        Request(
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityRequest)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[AlgorithmError]](
        response,
        Status.BadRequest,
        Some(
          List(
            AlgorithmError.AlgorithmIdIsNotAlphaNumerical(
              AlgorithmError.AlgorithmIdIsNotAlphaNumerical.message(id)
            )
          )
        )
      )
    )
  }

  it should "fail to create an algorithm because the protocol is not compatible" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor = ProjectUtils.createLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val id = "testid"
    val projectId = project.id
    val backend = RasaNluClassificationBackend(
      "grpc://test",
      "myproject",
      "mymodel",
      RasaNluFeaturesTransformer(
        field = "",
        joinCharacter = ""
      ),
      RasaNluLabelsTransformer()
    )
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      id,
      projectId,
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(id, backend, projectId, security)
    val response = algorithmsController.service
      .run(
        Request(
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityRequest)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[AlgorithmError]](
        response,
        Status.BadRequest,
        Some(
          List(
            AlgorithmError.UnsupportedProtocolError(
              AlgorithmError.UnsupportedProtocolError.message("grpc")
            )
          )
        )
      )
    )
  }
}
