package com.foundaml.server.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.circe.jsonOf
import com.foundaml.server.domain.models._
import org.http4s.EntityDecoder
import cats.effect.IO
import cats.implicits._

object ProjectSerializer {

  import io.circe._

  implicit val algorithmPolicyEncoder: Encoder[AlgorithmPolicy] =
    AlgorithmPolicySerializer.Implicits.encoder
  implicit val algorithmPolicyDecoder: Decoder[AlgorithmPolicy] =
    AlgorithmPolicySerializer.Implicits.decoder

  implicit val problemTypeEncoder: Encoder[ProblemType] =
    ProblemTypeSerializer.encoder
  implicit val problemTypeDecoder: Decoder[ProblemType] =
    ProblemTypeSerializer.decoder

  implicit val algorithmEncoder: Encoder[Algorithm] =
    AlgorithmsSerializer.encoder
  implicit val algorithmDecoder: Decoder[Algorithm] =
    AlgorithmsSerializer.decoder

  implicit val featuresConfigurationEncoder: Encoder[FeaturesConfiguration] =
    FeaturesConfigurationSerializer.encoder
  implicit val featuresConfigurationDecoder: Decoder[FeaturesConfiguration] =
    FeaturesConfigurationSerializer.decoder

  implicit val projectConfigurationEncoder: Encoder[ProjectConfiguration] =
    ProjectConfigurationSerializer.encoder
  implicit val projectConfigurationDecoder: Decoder[ProjectConfiguration] =
    ProjectConfigurationSerializer.decoder

  implicit val algorithmListDecoder: Decoder[List[Algorithm]] =
    Decoder.decodeList[Algorithm](AlgorithmsSerializer.decoder)
  implicit val algorithmListEncoder: Encoder[List[Algorithm]] =
    Encoder.encodeList[Algorithm](AlgorithmsSerializer.encoder)

  implicit val encoder: Encoder[Project] =
    (project: Project) =>
      Json.obj(
        ("id", Json.fromString(project.id)),
        ("name", Json.fromString(project.name)),
        ("problem", ProblemTypeSerializer.encodeJson(project.problem)),
        ("algorithms", algorithmListEncoder(project.algorithms)),
        ("policy", AlgorithmPolicySerializer.encodeJson(project.policy)),
        (
          "configuration",
          ProjectConfigurationSerializer.encodeJson(project.configuration)
        )
      )

  implicit val decoder: Decoder[Project] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        name <- c.downField("name").as[String]
        problem <- c.downField("problem").as[ProblemType]
        algorithms <- c.downField("algorithms").as[Option[List[Algorithm]]]
        policy <- c.downField("policy").as[Option[AlgorithmPolicy]]
        result <- problem match {
          case Classification =>
            c.downField("configuration")
              .as[ClassificationConfiguration](
                ProjectConfigurationSerializer.classificationConfigurationDecoder
              )
              .flatMap { classificationConfiguration =>
                Right(
                  ClassificationProject(
                    id,
                    name,
                    classificationConfiguration,
                    algorithms,
                    policy
                  )
                )
              }

          case Regression =>
            c.downField("configuration")
              .as[RegressionConfiguration](
                ProjectConfigurationSerializer.regressionConfigurationDecoder
              )
              .flatMap { regressionConfiguration =>
                Right(
                  RegressionProject(
                    id,
                    name,
                    regressionConfiguration,
                    algorithms,
                    policy
                  )
                )
              }
        }

      } yield result

  implicit val entityDecoder: EntityDecoder[IO, Project] =
    jsonOf[IO, Project]

  def encodeJson(project: Project): Json = {
    project.asJson
  }

  def decodeJson(n: String): Project = {
    decode[Project](n).right.get
  }
}
