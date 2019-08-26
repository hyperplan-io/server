/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.infrastructure.serialization

import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import com.hyperplan.domain.models._
import org.http4s.{EntityDecoder, EntityEncoder}
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

  implicit val featuresConfigurationEncoder: Encoder[FeatureVectorDescriptor] =
    FeaturesConfigurationSerializer.encoder
  implicit val featuresConfigurationDecoder: Decoder[FeatureVectorDescriptor] =
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

  implicit val entityListDecoder: EntityDecoder[IO, List[Project]] =
    jsonOf[IO, List[Project]]

  implicit val entityEncoder: EntityEncoder[IO, Project] =
    jsonEncoderOf[IO, Project]

  implicit val entityListEncoder: EntityEncoder[IO, List[Project]] =
    jsonEncoderOf[IO, List[Project]]

  def encodeJson(project: Project): Json = {
    project.asJson
  }

  def encodeJsonList(projects: List[Project]): Json = {
    projects.asJson
  }

  def decodeJson(n: String): Project = {
    decode[Project](n).right.get
  }
}
