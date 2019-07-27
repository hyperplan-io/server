package com.hyperplan.test.application

import cats.effect.IO
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.FlatSpec

import scalacache._
import scalacache.caffeine._
import scalacache.CatsEffect.modes._

import org.scalatest.Matchers
import org.scalatest._
import com.hyperplan.test.TestDatabase
import com.hyperplan.application.controllers.{
  FeaturesController,
  LabelsController,
  ProjectsController
}
import com.hyperplan.domain.errors.{ProjectError, _}
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.services._
import com.hyperplan.domain.repositories._
import com.hyperplan.infrastructure.serialization.errors.{
  ErrorsSerializer,
  ProjectErrorsSerializer
}
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.application.controllers.requests.{
  PatchProjectRequest,
  PostProjectRequest
}
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.models
import com.hyperplan.domain.models.backends.LocalClassification

class ProjectsControllerSpec()
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestDatabase {

  override def beforeAll(): Unit = initSchema()
  override def beforeEach(): Unit = wipeTables()

  /**
    * Project
    */
  implicit val projectEntityDecoder: EntityDecoder[IO, Project] =
    ProjectSerializer.entityDecoder
  implicit val projectListEntityDecoder: EntityDecoder[IO, List[Project]] =
    ProjectSerializer.entityListDecoder
  implicit val projectEntityEncoder: EntityEncoder[IO, Project] =
    ProjectSerializer.entityEncoder
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

  it should "not return a project that does not exist" in {
    val response = projectsController.service
      .run(
        Request(
          method = Method.GET,
          uri = uri"/my-id"
        )
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.NotFound,
        Some(
          List(
            ProjectDoesNotExistError(
              "The project my-id does not exist"
            )
          )
        )
      )
    )
  }

  it should "fail to create a project because id is empty" in {

    val entityBody = PostProjectRequest(
      id = "",
      name = "my-classification-project",
      problem = Classification,
      featuresId = "test-features",
      labelsId = "test-labels".some,
      topic = "test-topic".some
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectIdIsEmptyError()
          )
        )
      )
    )
  }

  it should "fail to create a project because id not alpha numeric" in {
    val id = "&$dfknd"
    val entityBody = PostProjectRequest(
      id = id,
      name = "my-classification-project",
      problem = Classification,
      featuresId = "test-features",
      labelsId = "test-labels".some,
      topic = "test-topic".some
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectIdIsNotAlphaNumericalError(
              ProjectIdIsNotAlphaNumericalError.message(id)
            )
          )
        )
      )
    )
  }

  it should "fail to create a project because id contains a space" in {
    val id = "my id"
    val entityBody = PostProjectRequest(
      id = id,
      name = "my-classification-project",
      problem = Classification,
      featuresId = "test-features",
      labelsId = "test-labels".some,
      topic = "test-topic".some
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectIdIsNotAlphaNumericalError(
              ProjectIdIsNotAlphaNumericalError.message(id)
            )
          )
        )
      )
    )
  }

  it should "fail to create a project because features do not exist" in {

    val entityBody = PostProjectRequest(
      id = "test",
      name = "my-classification-project",
      problem = Classification,
      featuresId = "test-features",
      labelsId = "test-labels".some,
      topic = "test-topic".some
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            FeaturesDoesNotExistError(
              FeaturesDoesNotExistError.message(entityBody.featuresId)
            )
          )
        )
      )
    )
  }

  it should "fail to create a project because labels do not exist" in {

    val id = "test"
    val name = "my classification project"
    val problem = Classification
    val featuresId = "my-feature-id"
    val labelsId = "my-label-id".some
    val topic = "my-topic".some

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = labelsId,
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectError.LabelsDoesNotExistError(
              ProjectError.LabelsDoesNotExistError.message(labelsId.get)
            )
          )
        )
      )
    )
  }

  it should "fail to create a classification project because labels are not set" in {

    val entityBody = PostProjectRequest(
      id = "test",
      name = "my-classification-project",
      problem = Classification,
      featuresId = "test-features",
      labelsId = none[String],
      topic = "test-topic".some
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectLabelsAreRequiredForClassificationError()
          )
        )
      )
    )
  }

  it should "successfully create a classification project with oneOf labels" in {

    val id = "test"
    val name = "my classification project"
    val problem = Classification
    val featuresId = "my-feature-id"
    val labelsId = "my-label-id".some
    val topic = "my-topic".some

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBodyLabels = LabelVectorDescriptor(
      id = labelsId.get,
      data = OneOfLabelsDescriptor(
        oneOf = Set("label-1", "label-2"),
        description = "my description"
      )
    )

    labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyLabels)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = labelsId,
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[Project](
        response,
        Status.Created,
        Some(
          models.ClassificationProject(
            id,
            name,
            ClassificationConfiguration(
              entityBodyFeatures,
              entityBodyLabels,
              StreamConfiguration(topic.get).some
            ),
            Nil,
            NoAlgorithm()
          )
        )
      )
    )
  }

  it should "successfully create a classification project with dynamic labels" in {

    val id = "test"
    val name = "my classification project"
    val problem = Classification
    val featuresId = "my-feature-id"
    val labelsId = "my-label-id".some
    val topic = none[String]

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBodyLabels = LabelVectorDescriptor(
      id = labelsId.get,
      data = DynamicLabelsDescriptor(
        description = "my description"
      )
    )

    labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyLabels)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = labelsId,
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
    assert(
      ControllerTestUtils.check[Project](
        response,
        Status.Created,
        Some(
          models.ClassificationProject(
            id,
            name,
            ClassificationConfiguration(
              entityBodyFeatures,
              entityBodyLabels,
              none[StreamConfiguration]
            ),
            Nil,
            NoAlgorithm()
          )
        )
      )
    )
  }

  it should "successfully create a regression project" in {

    val id = "test"
    val name = "my regression project"
    val problem = Regression
    val featuresId = "my-feature-id"
    val topic = "my-topic".some

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = none[String],
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[Project](
        response,
        Status.Created,
        Some(
          models.RegressionProject(
            id,
            name,
            RegressionConfiguration(
              entityBodyFeatures,
              StreamConfiguration(topic.get).some
            ),
            Nil,
            NoAlgorithm()
          )
        )
      )
    )
  }

  it should "successfully create projects and read them" in {
    val featuresId = "my-feature-id"
    val labelsId = "my-label-id".some
    val topic = "my-topic".some

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBodyLabels = LabelVectorDescriptor(
      id = labelsId.get,
      data = DynamicLabelsDescriptor(
        description = "my description"
      )
    )

    labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyLabels)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody1 = PostProjectRequest(
      id = "testregression",
      name = "regression project",
      problem = Regression,
      featuresId = featuresId,
      labelsId = none[String],
      topic = topic
    )

    val entityBody2 = PostProjectRequest(
      id = "testclassification",
      name = "classification project",
      problem = Classification,
      featuresId = featuresId,
      labelsId = labelsId,
      topic = topic
    )

    val response1 = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody1)
      )
      .value
      .map(_.get)

    val response2 = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody2)
      )
      .value
      .map(_.get)

    val regressionProject = models.RegressionProject(
      entityBody1.id,
      entityBody1.name,
      RegressionConfiguration(
        entityBodyFeatures,
        StreamConfiguration(topic.get).some
      ),
      Nil,
      NoAlgorithm()
    )

    val classificationProject = models.ClassificationProject(
      entityBody2.id,
      entityBody2.name,
      ClassificationConfiguration(
        entityBodyFeatures,
        entityBodyLabels,
        StreamConfiguration(topic.get).some
      ),
      Nil,
      NoAlgorithm()
    )

    assert(
      ControllerTestUtils.check[Project](
        response1,
        Status.Created,
        regressionProject.some
      )
    )

    assert(
      ControllerTestUtils.check[Project](
        response2,
        Status.Created,
        classificationProject.some
      )
    )

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
        Some(
          List(
            regressionProject,
            classificationProject
          )
        )
      )
    )
  }

  it should "fail to update a project that does not exist" in {
    val response = projectsController.service
      .run(
        Request(
          method = Method.PATCH,
          uri = uri"/myproject"
        ).withEntity(
          PatchProjectRequest(
            "myproject".some,
            DefaultAlgorithm("test").some
          )
        )
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectDoesNotExistError(
              ProjectDoesNotExistError.message("myproject")
            )
          )
        )
      )
    )
  }

  it should "fail to update a project with an empty name" in {
    val response = projectsController.service
      .run(
        Request(
          method = Method.PATCH,
          uri = uri"/myproject"
        ).withEntity(
          PatchProjectRequest(
            "".some,
            DefaultAlgorithm("test").some
          )
        )
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response,
        Status.BadRequest,
        Some(
          List(
            ProjectError.ProjectNameIsEmptyError()
          )
        )
      )
    )
  }

  it should "fail to update a project policy to default with an algorithm that does not exist" in {

    val id = "test"
    val name = "my regression project"
    val problem = Regression
    val featuresId = "my-feature-id"
    val topic = "my-topic".some

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = none[String],
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[Project](
        response,
        Status.Created,
        Some(
          models.RegressionProject(
            id,
            name,
            RegressionConfiguration(
              entityBodyFeatures,
              StreamConfiguration(topic.get).some
            ),
            Nil,
            NoAlgorithm()
          )
        )
      )
    )

    val response2 = projectsController.service
      .run(
        Request(
          method = Method.PATCH,
          uri = uri"/test"
        ).withEntity(
          PatchProjectRequest(
            none[String],
            DefaultAlgorithm("test").some
          )
        )
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response2,
        Status.BadRequest,
        Some(
          List(
            ProjectPolicyAlgorithmDoesNotExist(
              ProjectPolicyAlgorithmDoesNotExist.message("test")
            )
          )
        )
      )
    )
  }

  it should "fail to update a project policy to weighted with an algorithm that does not exist" in {

    val id = "test"
    val name = "my regression project"
    val problem = Regression
    val featuresId = "my-feature-id"
    val topic = "my-topic".some

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = none[String],
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[Project](
        response,
        Status.Created,
        Some(
          models.RegressionProject(
            id,
            name,
            RegressionConfiguration(
              entityBodyFeatures,
              StreamConfiguration(topic.get).some
            ),
            Nil,
            NoAlgorithm()
          )
        )
      )
    )

    val response2 = projectsController.service
      .run(
        Request(
          method = Method.PATCH,
          uri = uri"/test"
        ).withEntity(
          PatchProjectRequest(
            none[String],
            models
              .WeightedAlgorithm(
                List(
                  AlgorithmWeight(
                    "algo1",
                    0.5f
                  ),
                  AlgorithmWeight(
                    "algo2",
                    0.5f
                  )
                )
              )
              .some
          )
        )
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[List[ProjectError]](
        response2,
        Status.BadRequest,
        Some(
          List(
            ProjectPolicyAlgorithmDoesNotExist(
              ProjectPolicyAlgorithmDoesNotExist.message(Seq("algo1", "algo2"))
            )
          )
        )
      )
    )
  }
  // TODO uncomment when algorithm service is refactored
  /*
  it should "successfully update a project policy to default algorithm" in {

    val id = "test"
    val name = "my regression project"
    val problem = Regression
    val featuresId = "my-feature-id"
    val topic = "my-topic".some

    val algorithmId = "my-algorithm1"

    val entityBodyFeatures = FeatureVectorDescriptor(
      id = featuresId,
      data = List(
        FeatureDescriptor(
          name = "feature-1",
          featuresType = StringFeatureType,
          dimension = Scalar,
          description = "my description"
        )
      )
    )

    featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    val entityBody = PostProjectRequest(
      id = id,
      name = name,
      problem = problem,
      featuresId = featuresId,
      labelsId = none[String],
      topic = topic
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[Project](
        response,
        Status.Created,
        Some(
          models.RegressionProject(
            id,
            name,
            RegressionConfiguration(
              entityBodyFeatures,
              StreamConfiguration(topic.get).some
            ),
            Nil,
            NoAlgorithm()
          )
        )
      )
    )

    algorithmsService.createAlgorithm(
      algorithmId,
      LocalClassification(
        Set.empty
      ),
      id,
      PlainSecurityConfiguration(Nil)
    ).unsafeRunSync()

    projectCache.remove[IO]("test").unsafeRunSync()
    val response2 = projectsController.service
      .run(
        Request(
          method = Method.PATCH,
          uri = uri"/test"
        ).withEntity(
          PatchProjectRequest(
            none[String],
            models
              .DefaultAlgorithm(
                algorithmId
              )
              .some
          )
        )
      )
      .value
      .map(_.get)

    assert(
      ControllerTestUtils.check[Project](
        response2,
        Status.Ok,
        Some(
          models.RegressionProject(
            id,
            name,
            RegressionConfiguration(
              entityBodyFeatures,
              StreamConfiguration(topic.get).some
            ),
            Nil,
            DefaultAlgorithm(algorithmId)
          )
        )
      )
    )
  }
 */
}
