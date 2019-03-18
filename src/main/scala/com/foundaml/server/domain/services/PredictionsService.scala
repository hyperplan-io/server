package com.foundaml.server.domain.services

import java.util.UUID

import com.foundaml.server.domain.{FoundaMLConfig, models}
import com.foundaml.server.domain.factories.ProjectFactory
import org.http4s._
import scalaz.zio.{IO, Task}
import scalaz.zio.interop.catz._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.backends._
import com.foundaml.server.domain.models.errors._
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.models.labels._
import com.foundaml.server.domain.models.labels.transformers.TensorFlowLabels
import com.foundaml.server.domain.repositories.{PredictionsRepository, ProjectsRepository}
import com.foundaml.server.infrastructure.serialization.{PredictionSerializer, TensorFlowFeaturesSerializer, TensorFlowLabelsSerializer}
import com.foundaml.server.infrastructure.streaming.KinesisService
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

class PredictionsService(
    projectsRepository: ProjectsRepository,
    predictionsRepository: PredictionsRepository,
    kinesisService: KinesisService,
    projectFactory: ProjectFactory,
    config: FoundaMLConfig
) {

  def persistPrediction(prediction: Prediction) =
    predictionsRepository.insert(prediction)

  def publishPredictionToKinesis(prediction: Prediction) =
    if (config.kinesis.enabled) {
      kinesisService.put(
        prediction,
        config.kinesis.predictionsStream,
        prediction.projectId
      )(PredictionSerializer.encoder)
    } else Task.unit

  def noAlgorithm(): Task[Prediction] =
    Task(println("No algorithm setup")).flatMap { _ =>
      Task.fail(
        NoAlgorithmAvailable("No algorithms are setup")
      )
    }

  def predictWithProjectPolicy(
      features: Features,
      project: Project
  ): Task[Prediction] =
    project.policy
      .take()
      .fold(
        noAlgorithm()
      ) { algorithmId =>
        project.algorithmsMap
          .get(algorithmId)
          .fold(
            noAlgorithm()
          )(
            algorithm =>
              predictWithAlgorithm(
                project.id,
                algorithm,
                features
              )
          )
      }

  def predictWithLocalBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      local: Local
  ) = {
    Task.succeed(
      Prediction(
        UUID.randomUUID().toString,
        projectId,
        algorithm.id,
        features,
        local.computed,
        Examples(Set.empty)
      )
    )
  }
  def predictWithTensorFlowBackend(
      projectId: String,
      algorithm: Algorithm,
      features: Features,
      backend: TensorFlowBackend
  ) = {
    val predictionId = UUID.randomUUID().toString
    backend.featuresTransformer
      .transform(features)
      .fold(
        err =>
          Task(println(err.getMessage)) *>
            Task.fail(
              FeaturesTransformerError(
                "The features could not be transformed to a TensorFlow compatible format"
              )
            ),
        tfFeatures => {
          implicit val encoder
              : EntityEncoder[Task, TensorFlowClassificationFeatures] =
            TensorFlowFeaturesSerializer.entityEncoder
          val uriString = s"http://${backend.host}:${backend.port}"
          Uri
            .fromString(uriString)
            .fold(
              _ =>
                Task.fail(
                  InvalidArgument(
                    s"The following uri could not be parsed, check your backend configuration. $uriString"
                  )
                ),
              uri => {
                val request =
                  Request[Task](method = Method.POST, uri = uri)
                    .withBody(tfFeatures)
                BlazeClientBuilder[Task](ExecutionContext.global).resource
                  .use(
                    _.expect[TensorFlowLabels](request)(
                      TensorFlowLabelsSerializer.entityDecoder
                    ).flatMap { tfLabels =>
                      backend.labelsTransformer
                        .transform(predictionId, tfLabels)
                        .fold(
                          err => Task.fail(err),
                          labels =>
                            Task.succeed(
                              Prediction(
                                predictionId,
                                projectId,
                                algorithm.id,
                                features,
                                labels,
                                Examples(Set.empty)
                              )
                            )
                        )
                    }
                  )
              }
            )

        }
      )
  }

  def predictWithAlgorithm(
      projectId: String,
      algorithm: Algorithm,
      features: Features
  ): Task[Prediction] = {
    val predictionTask = algorithm.backend match {
      case local: Local =>
        predictWithLocalBackend(projectId, algorithm, features, local)
      case tfBackend: TensorFlowBackend =>
        predictWithTensorFlowBackend(projectId, algorithm, features, tfBackend)
    }
    predictionTask.flatMap { prediction =>
      persistPrediction(prediction) *> publishPredictionToKinesis(prediction) *> Task
        .succeed(prediction)
    }
  }

  def validateFeatures(
      expectedFeaturesClass: String,
      expectedFeaturesSize: Int,
      features: Features
  ): Boolean = {
    lazy val typeCheck = expectedFeaturesClass match {
      case DoubleFeatures.featuresClass =>
        features.data.count(_.isInstanceOf[Double]) == features.data.size
      case FloatFeatures.featuresClass =>
        features.data.count(_.isInstanceOf[Float]) == features.data.size
      case IntFeatures.featuresClass =>
        features.data.count(_.isInstanceOf[Int]) == features.data.size
      case StringFeatures.featuresClass =>
        features.data.count(_.isInstanceOf[String]) == features.data.size
      case CustomFeatures.featuresClass =>
        // custom features does not guarantee the features to be correct
        true
    }
    lazy val sizeCheck = features.data.size == expectedFeaturesSize

    sizeCheck && typeCheck
  }

  def validateLabels(
      expectedLabelsClass: Set[String],
      labels: Labels
  ): Boolean = {
    expectedLabelsClass == labels.labels.map(_.label)
  }

  def predict(
    projectId: String,
    features: Features,
    optionalAlgorithmId: Option[String]
  ) = projectFactory.get(projectId).flatMap { project =>
      predictForProject(project, features, optionalAlgorithmId)
    }

  def predictForProject(
               project: Project,
      features: Features,
      optionalAlgorithmId: Option[String]
  ) = {

    if (validateFeatures(
        project.configuration.featureClass,
        project.configuration.featuresSize,
        features
      )) {
      optionalAlgorithmId.fold(
        predictWithProjectPolicy(features, project)
      )(
        algorithmId =>
          project.algorithmsMap
            .get(algorithmId)
            .fold[Task[Prediction]](
              Task(
                println(
                  s"project algorithms: ${project.algorithmsMap.toString()}"
                )
              ) *> Task.fail(
                InvalidArgument(
                  s"The algorithm $algorithmId does not exist in the project ${project.id}"
                )
              )
            )(
              algorithm =>
                predictWithAlgorithm(project.id, algorithm, features).flatMap {
                  prediction =>
                    if (validateLabels(
                        project.configuration.labels,
                        prediction.labels
                      )) {
                      Task.succeed(prediction)
                    } else {
                      Task.fail(
                        LabelsValidationFailed(
                          "The labels do not match the project configuration"
                        )
                      )
                    }
                }
            )
      )
    } else {
      Task.fail(
        FeaturesValidationFailed(
          "The features are not correct for this project"
        )
      )
    }
  }

  def addExample(predictionId: String, labelId: String) =
    predictionsRepository.read(predictionId).fold[Task[Label]](
      err => Task.fail(NotFound(s"The prediction $predictionId does not exist")),
      prediction => prediction.labels.labels.find(_.id == labelId).fold[Task[Label]](
        Task.fail(NotFound(s"The label $labelId does not exist"))
      ) (
        label => {
          val updatedPrediction = prediction.copy(examples = Examples(prediction.examples.examples + label))
          predictionsRepository.updateExamples(predictionId, updatedPrediction.labels) *> Task.succeed(label)
        }
      )
    )
}
