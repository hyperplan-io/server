package com.hyperplan.test

import cats.effect.IO
import cats.implicits._

import org.http4s._
import org.http4s.implicits._

import com.hyperplan.domain.models._
import com.hyperplan.domain.models.features._
import com.hyperplan.domain.models.labels._

import com.hyperplan.application.controllers.requests._
import com.hyperplan.infrastructure.serialization._
import com.hyperplan.application.controllers._
import com.hyperplan.domain.models._

object ProjectUtils {

  implicit val postProjectRequestEntityEncoder
      : EntityEncoder[IO, PostProjectRequest] =
    PostProjectRequestSerializer.entityEncoder

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

  def createLabels(
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

}
