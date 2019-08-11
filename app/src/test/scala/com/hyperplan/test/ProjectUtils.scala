package com.hyperplan.test

import cats.effect.IO
import cats.implicits._
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.labels._
import com.hyperplan.application.controllers.requests._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.application.controllers._
import com.hyperplan.domain.models._
import com.hyperplan.domain.models.backends.LocalRandomClassification
import io.circe.{Encoder, Json}

import scala.util.Random
import com.hyperplan.domain.models.backends.TensorFlowClassificationBackend
import com.hyperplan.domain.models.features.transformers.TensorFlowFeaturesTransformer
import com.hyperplan.domain.models.labels.transformers.TensorFlowLabelsTransformer

object ProjectUtils {

  implicit val postProjectRequestEntityEncoder
      : EntityEncoder[IO, PostProjectRequest] =
    PostProjectRequestSerializer.entityEncoder

  implicit val postAlgorithmRequestEntityEncoder
      : EntityEncoder[IO, PostAlgorithmRequest] =
    PostAlgorithmRequestEntitySerializer.entityEncoder

  implicit val featuresConfigurationEncoder =
    FeaturesConfigurationSerializer.entityEncoder

  implicit val featuresConfigurationDecoder =
    FeaturesConfigurationSerializer.entityDecoder

  implicit val labelsConfigurationEncoder =
    LabelsConfigurationSerializer.entityEncoder

  implicit val labelsConfigurationDecoder =
    LabelsConfigurationSerializer.entityDecoder

  implicit val projectDecoder =
    ProjectSerializer.entityDecoder

  implicit val algorithmDecoder =
    AlgorithmsSerializer.entityDecoder

  case class FakePredictionRequest(
      projectId: String,
      algorithmId: Option[String],
      entityLinks: Option[List[EntityLink]],
      features: List[Feature]
  )

  implicit val fakePredictionEncoder: Encoder[FakePredictionRequest] =
    (r: FakePredictionRequest) =>
      Json
        .obj(
          ("projectId", Json.fromString(r.projectId)),
          (
            "algorithmId",
            r.algorithmId
              .fold(Json.Null)(algorithmId => Json.fromString(algorithmId))
          ),
          (
            "entityLinks",
            r.entityLinks
              .fold(Json.Null)(links => entityLinkEncoder.apply(links))
          )
        )
        .deepMerge(
          Json.obj(
            "features" -> r.features
              .map(featureEncoder.apply)
              .reduce((j1, j2) => j1.deepMerge(j2))
          )
        )

  implicit val entityEncoder = jsonEncoderOf[IO, FakePredictionRequest]
  implicit val entityLinkEncoder: Encoder[List[EntityLink]] =
    PredictionSerializer.entityLinkListEncoder

  implicit val featureEncoder: Encoder[Feature] = {
    case FloatFeature(key, value) =>
      Json.obj((key, Json.fromFloatOrNull(value)))
    case FloatArrayFeature(key, data) =>
      Json.obj((key, Json.arr(data.map(Json.fromFloatOrNull): _*)))
    case FloatMatrixFeature(key, data) =>
      Json.obj(
        (
          key,
          Json.arr(
            data.flatMap(nestedData => nestedData.map(Json.fromFloatOrNull)): _*
          )
        )
      )
    case IntFeature(key, value) => Json.obj((key, Json.fromInt(value)))
    case IntArrayFeature(key, data) =>
      Json.obj((key, Json.arr(data.map(Json.fromInt): _*)))
    case IntMatrixFeature(key, data) =>
      Json.obj(
        (
          key,
          Json.arr(data.flatMap(nestedData => nestedData.map(Json.fromInt)): _*)
        )
      )
    case StringFeature(key, value) => Json.obj((key, Json.fromString(value)))
    case StringArrayFeature(key, data) =>
      Json.obj((key, Json.arr(data.map(Json.fromString): _*)))
    case StringMatrixFeature(key, data) =>
      Json.obj(
        (
          key,
          Json.arr(
            data.flatMap(nestedData => nestedData.map(Json.fromString)): _*
          )
        )
      )
    case ReferenceFeature(key, reference, value) =>
      Json.obj()
  }

  def genPredictionRequest(
      projectId: String,
      algorithmId: Option[String] = None
  ): FakePredictionRequest =
    FakePredictionRequest(
      projectId,
      algorithmId,
      None,
      List(
        StringFeature(
          "feature-1",
          Random.alphanumeric.take(10).mkString
        )
      )
    )

  def createFeatures(
      featuresController: FeaturesController
  ): FeatureVectorDescriptor = {
    val featuresId = "myfeatureid"

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

    val response = featuresController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyFeatures)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    response.attemptAs[FeatureVectorDescriptor].value.unsafeRunSync().right.get
  }

  def createDynamicLabels(
      labelsController: LabelsController
  ): LabelVectorDescriptor = {

    val entityBodyLabels = LabelVectorDescriptor(
      id = "mylabelid",
      data = DynamicLabelsDescriptor(
        description = "my description"
      )
    )

    val response = labelsController.service
      .run(
        Request[IO](
          method = Method.POST,
          uri = uri"/"
        ).withEntity(entityBodyLabels)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    response.attemptAs[LabelVectorDescriptor].value.unsafeRunSync().right.get

  }

  def createClassificationProject(
      projectsController: ProjectsController,
      featureVectorDescriptor: FeatureVectorDescriptor,
      labelVectorDescriptor: LabelVectorDescriptor
  ): ClassificationProject = {

    val entityBody = PostProjectRequest(
      id = "testclassification",
      name = "classificationproject",
      problem = Classification,
      featuresId = featureVectorDescriptor.id,
      labelsId = labelVectorDescriptor.id.some,
      topic = none[String]
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
      .unsafeRunSync()

    response
      .attemptAs[Project]
      .value
      .unsafeRunSync()
      .right
      .get
      .asInstanceOf[ClassificationProject]
  }

  def createRegressionProject(
      projectsController: ProjectsController,
      featureVectorDescriptor: FeatureVectorDescriptor
  ): RegressionProject = {

    val entityBody = PostProjectRequest(
      id = "testregression",
      name = "regressionproject",
      problem = Regression,
      featuresId = featureVectorDescriptor.id,
      labelsId = None,
      topic = none[String]
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
      .unsafeRunSync()

    response
      .attemptAs[Project]
      .value
      .unsafeRunSync()
      .right
      .get
      .asInstanceOf[RegressionProject]
  }

  def createAlgorithmLocalClassification(
      projectsController: ProjectsController,
      project: Project
  ): Algorithm = {

    val algorithmId = Random.alphanumeric.take(10).mkString("")
    val projectId = project.id
    val entityBody = PostAlgorithmRequest(
      LocalRandomClassification(Set.empty),
      PlainSecurityConfiguration(
        Nil
      )
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    response
      .attemptAs[Algorithm]
      .value
      .unsafeRunSync()
      .right
      .get
  }

  def createAlgorithmTensorFlowClassification(
      projectsController: ProjectsController,
      project: Project
  ): Algorithm = {
    val algorithmId = Random.alphanumeric.take(10).mkString("")
    val projectId = project.id
    val entityBody = PostAlgorithmRequest(
      TensorFlowClassificationBackend(
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
      ),
      PlainSecurityConfiguration(
        Nil
      )
    )

    val response = projectsController.service
      .run(
        Request[IO](
          method = Method.PUT,
          uri = uri"" / projectId / "algorithms" / algorithmId
        ).withEntity(entityBody)
      )
      .value
      .map(_.get)
      .unsafeRunSync()

    response
      .attemptAs[Algorithm]
      .value
      .unsafeRunSync()
      .right
      .get
  }
}
