package com.hyperplan.test.application

import cats.effect.{IO, Resource}
import cats.implicits._

import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import org.scalatest.{FlatSpec, Matchers, _}
import org.scalatest.Inside._

import pureconfig.generic.auto._
import scalacache._
import scalacache.caffeine._

import com.hyperplan.application.ApplicationConfig
import com.hyperplan.application.controllers.requests.{
  PatchProjectRequest,
  PostProjectRequest,
  PredictionRequest
}
import com.hyperplan.application.controllers._
import com.hyperplan.domain.errors.{PredictionError, ProjectError}
import com.hyperplan.domain.models._
import com.hyperplan.domain.repositories._
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.infrastructure.serialization.errors.{
  ErrorsSerializer,
  PredictionErrorsSerializer,
  ProjectErrorsSerializer
}
import com.hyperplan.test.{ProjectUtils, TestDatabase}

import scala.concurrent.ExecutionContext

class PredictionsControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  override def beforeAll(): Unit = initSchema()
  override def beforeEach(): Unit = wipeTables()

  implicit val timer = IO.timer(ExecutionContext.global)
  // context shift is inherited from trait TestDatabase

  /**
    * Prediction
    */
  implicit val predictionEntityEncoder: EntityEncoder[IO, Prediction] =
    PredictionSerializer.entityEncoder
  implicit val predictionEntityDecoder: EntityDecoder[IO, Prediction] =
    PredictionSerializer.entityDecoder
  implicit val predictionListEntityDecoder
      : EntityDecoder[IO, List[Prediction]] =
    PredictionSerializer.entityListDecoder
  implicit val predictionListEntityEncoder
      : EntityEncoder[IO, List[Prediction]] =
    PredictionSerializer.entityListEncoder

  implicit val predictionErrorsDecoder
      : EntityDecoder[IO, List[PredictionError]] =
    PredictionErrorsSerializer.predictionErrorsEntityDecoder

  /**
    * Project
    */
  implicit val projectEntityDecoder: EntityDecoder[IO, Project] =
    ProjectSerializer.entityDecoder
  implicit val projectListEntityDecoder: EntityDecoder[IO, List[Project]] =
    ProjectSerializer.entityListDecoder
  implicit val projectListEntityEncoder: EntityEncoder[IO, List[Project]] =
    ProjectSerializer.entityListEncoder

  implicit val projectErrorDecoder: EntityDecoder[IO, List[ProjectError]] =
    ProjectErrorsSerializer.projectErrorEntityDecoder

  /**
    * Post project request
    */
  implicit val postProjectRequestEntityEncoder
      : EntityEncoder[IO, PostProjectRequest] =
    PostProjectRequestSerializer.entityEncoder

  /**
    * Patch project request
    */
  implicit val patchProjectRequestEntityEncoder
      : EntityEncoder[IO, PatchProjectRequest] =
    PatchProjectRequestSerializer.entityEncoder
  implicit val patchProjectRequestEntityDecoder
      : EntityDecoder[IO, PatchProjectRequest] =
    PatchProjectRequestSerializer.entityDecoder

  /**
    * Features
    */
  implicit val featuresDecoderList =
    FeaturesConfigurationSerializer.entityDecoderList
  implicit val featuresDecoder = FeaturesConfigurationSerializer.entityDecoder
  implicit val featuresErrorDecoder = ErrorsSerializer.featureErrorEntityDecoder
  implicit val featuresConfigurationEncoder =
    FeaturesConfigurationSerializer.entityEncoder

  /**
    * Labels
    */
  implicit val labelsDecoderList =
    LabelsConfigurationSerializer.entityDecoderList
  implicit val labelsDecoder = LabelsConfigurationSerializer.entityDecoder
  implicit val labelsErrorDecoder = ErrorsSerializer.labelErrorEntityDecoder
  implicit val labelsConfigurationEncoder =
    LabelsConfigurationSerializer.entityEncoder

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

  val predictionsController = new PredictionsController(
    projectsService,
    domainService,
    predictionsService
  )

  it should "fail to execute a prediction when selecting an algorithm that does not exist" in {
    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor =
      ProjectUtils.createDynamicLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )

    val response = predictionsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(
          ProjectUtils.genPredictionRequest(project.id, "someid".some)
        )
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[List[PredictionError]](
        response,
        Status.BadRequest,
        Some(
          List(
            PredictionError.AlgorithmDoesNotExistError(
              PredictionError.AlgorithmDoesNotExistError.message("someid")
            )
          )
        )
      )
    )
  }

  it should "execute first algorithm by default" in {
    val featureVectorDescriptor =
      ProjectUtils.createFeatures(featuresController)
    val labelVectorDescriptor =
      ProjectUtils.createDynamicLabels(labelsController)
    val project = ProjectUtils.createClassificationProject(
      projectsController,
      featureVectorDescriptor,
      labelVectorDescriptor
    )
    val algorithm1 = ProjectUtils.createAlgorithmLocalClassification(
      projectsController,
      project
    )
    val requestEntity1 = ProjectUtils.genPredictionRequest(project.id)
    val response1 = predictionsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(requestEntity1)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.checkWithFunc[Prediction](
        response1,
        Status.Created,
        (prediction: Prediction) => {
          inside(prediction) {
            case ClassificationPrediction(
                _,
                projectId,
                algorithmId,
                features,
                examples,
                labels
                ) =>
              projectId should be(requestEntity1.projectId)
              algorithmId should be(
                ProjectsServiceLive.defaultRandomAlgorithmId
              )
              features should be(requestEntity1.features)
              assert(examples.isEmpty)
              assert(labels.isEmpty)
          }
        }
      )
    )

    val algorithm2 = ProjectUtils.createAlgorithmLocalClassification(
      projectsController,
      project
    )
    val requestEntity2 = ProjectUtils.genPredictionRequest(project.id)
    val response2 = predictionsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(requestEntity2)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.checkWithFunc[Prediction](
        response2,
        Status.Created,
        (prediction: Prediction) => {
          inside(prediction) {
            case ClassificationPrediction(
                _,
                projectId,
                algorithmId,
                features,
                examples,
                labels
                ) =>
              projectId should be(requestEntity2.projectId)
              algorithmId should be(
                ProjectsServiceLive.defaultRandomAlgorithmId
              )
              features should be(requestEntity2.features)
              assert(examples.isEmpty)
              assert(labels.isEmpty)
          }
        }
      )
    )
  }

}
