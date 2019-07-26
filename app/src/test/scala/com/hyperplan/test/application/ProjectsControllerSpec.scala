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
import org.scalatest.Matchers
import org.scalatest._
import com.hyperplan.test.TestDatabase
import com.hyperplan.application.controllers.{
  FeaturesController,
  LabelsController,
  ProjectsController
}
import com.hyperplan.domain.errors._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.services._
import com.hyperplan.domain.repositories._
import com.hyperplan.infrastructure.serialization.errors.{
  ErrorsSerializer,
  ProjectErrorsSerializer
}
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.application.controllers.requests.PostProjectRequest
import com.hyperplan.domain.errors.ProjectError._
import com.hyperplan.domain.models

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

  /**
    * Post project request
    */
  implicit val postProjectRequestEntityEncoder
      : EntityEncoder[IO, PostProjectRequest] =
    PostProjectRequestSerializer.entityEncoder

  implicit val projectErrorDecoder =
    ProjectErrorsSerializer.projectErrorEntityDecoder

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
  val domainService = new DomainService(domainRepository)

  val projectCache: Cache[Project] = CaffeineCache[Project]
  val projectsService = new ProjectsService(
    projectRepository,
    domainService,
    projectCache
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

}
