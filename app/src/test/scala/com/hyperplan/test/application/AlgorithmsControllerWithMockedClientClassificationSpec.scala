package com.hyperplan.test.application

import cats.effect.{IO, Resource}
import pureconfig.generic.auto._
import org.http4s._
import org.http4s.client.Client
import org.scalatest.{FlatSpec, Matchers, _}
import scalacache._
import scalacache.caffeine._
import com.hyperplan.application.ApplicationConfig
import com.hyperplan.application.controllers.requests.PostAlgorithmRequest
import com.hyperplan.application.controllers.{
  FeaturesController,
  LabelsController,
  ProjectsController
}
import com.hyperplan.domain.errors.AlgorithmError
import com.hyperplan.domain.models.backends._
import com.hyperplan.domain.models.features.transformers.{
  RasaNluFeaturesTransformer,
  TensorFlowFeaturesTransformer
}
import com.hyperplan.domain.models.labels.transformers.{
  RasaNluLabelsTransformer,
  TensorFlowLabelsTransformer
}
import com.hyperplan.domain.models.{backends, _}
import com.hyperplan.domain.repositories._
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.errors.AlgorithmErrorsSerializer
import com.hyperplan.test._

import scala.concurrent.ExecutionContext

class AlgorithmsControllerWithMockedClientClassificationSpec()
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

  val app = HttpApp.pure(
    Response[IO](Status.Ok).withEntity(
      TensorFlowClassificationEntityResponse(
        List(
          TensorFlowClassificationResult(
            "feature-1",
            "0.5f"
          )
        )
      )
    )
  )
  val client = Client.fromHttpApp(app)
  val blazeClient = Resource.make(IO(client))(_ => IO.unit)

  val domainRepository = new DomainRepository()(xa)
  val projectRepository = new ProjectsRepository(domainRepository)(xa)
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

  it should "successfully to create an algorithm with Tensorflow classification" in {

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
        project.configuration.features.data.map { featureDescriptor =>
          featureDescriptor.name -> featureDescriptor.name
        }.toMap
      ),
      TensorFlowLabelsTransformer(
        project.configuration.labels.data match {
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
      ControllerTestUtils.check[Algorithm](
        response,
        Status.Created,
        Some(
          expectedAlgorithm
        )
      )
    )
  }

}
