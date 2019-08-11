package com.hyperplan.test.application

import cats.effect.IO
import cats.implicits._

import org.http4s._

import pureconfig.generic.auto._
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
import com.hyperplan.domain.models.backends.LocalRandomClassification
import com.hyperplan.application.controllers.requests.PostAlgorithmRequest
import com.hyperplan.test.ProjectUtils
import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.infrastructure.serialization.errors.AlgorithmErrorsSerializer
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.features.transformers.RasaNluFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.RasaNluLabelsTransformer
import cats.effect.Resource
import org.http4s.client.Client
import scala.concurrent.ExecutionContext
import org.http4s.client.blaze.BlazeClientBuilder
import cats.effect.ContextShift
import com.hyperplan.application.ApplicationConfig
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer

class AlgorithmsControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  implicit val timer = IO.timer(ExecutionContext.global)
  // context shift is inherited from trait TestDatabase

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

  val blazeClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](
    ExecutionContext.global
  ).resource

  val projectRepository = new ProjectsRepository()(xa)
  val domainRepository = new DomainRepository()(xa)
  val predictionsRepository = new PredictionsRepository()(xa)

  val domainService = new DomainServiceLive(domainRepository)
  val backendService = new BackendServiceLive(blazeClient)

  val projectCache: Cache[Project] = CaffeineCache[Project]
  val projectsService = new ProjectsServiceLive(
    projectRepository,
    domainService,
    backendService,
    projectCache
  )

  val config = pureconfig.loadConfig[ApplicationConfig].right.get

  val predictionsService = new PredictionsServiceLive(
    predictionsRepository,
    projectsService,
    backendService,
    None,
    None,
    None,
    config
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

  it should "fail to create an algorithm for a project that does not exist" in {
    val algorithmId = "test"
    val backend = LocalRandomClassification(Set.empty)
    val projectId = "myproject"
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(algorithmId, backend, projectId, security)
    val response = projectsController.service
      .run(
        Request(
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
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

  it should "successfully create an algorithm with local classification backend" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor =
      ProjectUtils.createDynamicLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val algorithmId = "test"
    val projectId = project.id
    val backend = LocalRandomClassification(Set.empty)
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(algorithmId, backend, projectId, security)
    val response = projectsController.service
      .run(
        Request(
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
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

  it should "fail to create an algorithm with Tensorflow classification backend because of dry run" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor =
      ProjectUtils.createDynamicLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val algorithmId = "test"
    val projectId = project.id
    val backend = TensorFlowClassificationBackend(
      "http://0.0.0.0:7089",
      "myModel",
      None,
      TensorFlowFeaturesTransformer(
        "signature",
        project.configuration
          .asInstanceOf[ClassificationConfiguration]
          .features
          .data
          .map { featureDescriptor =>
            featureDescriptor.name -> featureDescriptor.name
          }
          .toMap
      ),
      TensorFlowLabelsTransformer(
        project.configuration
          .asInstanceOf[ClassificationConfiguration]
          .labels
          .data match {
          case DynamicLabelsDescriptor(description) => Map.empty
          case OneOfLabelsDescriptor(oneOf, description) =>
            oneOf.map(one => one -> one).toMap
        }
      )
    )
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(algorithmId, backend, projectId, security)
    val response = projectsController.service
      .run(
        Request(
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
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
            AlgorithmError.PredictionDryRunFailed(
              "The prediction dry run failed because Unkown error: Connection refused"
            )
          )
        )
      )
    )
  }

  it should "fail to create an algorithm because the id is not alpha numerical" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor =
      ProjectUtils.createDynamicLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val algorithmId = "&erpokq"
    val projectId = project.id
    val backend = LocalRandomClassification(Set.empty)
    val security = PlainSecurityConfiguration(
      Nil
    )

    val entityRequest = PostAlgorithmRequest(
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(algorithmId, backend, projectId, security)
    val response = projectsController.service
      .run(
        Request(
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
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
              AlgorithmError.AlgorithmIdIsNotAlphaNumerical.message(algorithmId)
            )
          )
        )
      )
    )
  }

  it should "fail to create an algorithm because the protocol is not compatible" in {

    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor =
      ProjectUtils.createDynamicLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val algorithmId = "testid"
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
      backend,
      security
    )

    val expectedAlgorithm = Algorithm(algorithmId, backend, projectId, security)
    val response = projectsController.service
      .run(
        Request(
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
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
              AlgorithmError.UnsupportedProtocolError.message(Grpc)
            )
          )
        )
      )
    )
  }
}
