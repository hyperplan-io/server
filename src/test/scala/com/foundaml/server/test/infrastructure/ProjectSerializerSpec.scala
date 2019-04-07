package com.foundaml.server.test.infrastructure

import java.util.UUID

import com.foundaml.server.domain.models._
import com.foundaml.server.infrastructure.serialization.ProjectSerializer
import com.foundaml.server.test.SerializerTester
import io.circe.{Decoder, Encoder}
import org.scalatest.{FlatSpec, Matchers}

class ProjectSerializerSpec
    extends FlatSpec
    with SerializerTester
    with Matchers {

  val encoder: Encoder[Project] = ProjectSerializer.encoder
  val decoder: Decoder[Project] = ProjectSerializer.decoder

  it should "correctly encode a classification project" in {

    val projectId = "test-project-encode"
    val projectName = "test project"
    val configuration = ClassificationConfiguration(
      FeaturesConfiguration(
        List(
          FeatureConfiguration(
            "",
            "",
            ""
          )
        )
      ),
      OneOfLabelsConfiguration(
        Set(""),
        ""
      )
    )
    val project = ClassificationProject(
      projectId,
      projectName,
      configuration,
      Nil,
      NoAlgorithm()
    )

    testEncoder(project: Project) { json =>
      val expectedJson =
        """{"id":"test-project-encode","name":"test project","problem":"classification","algorithms":[],"policy":{"class":"NoAlgorithm"},"configuration":{"features":[{"name":"","type":"","description":""}],"labels":{"type":"oneOf","oneOf":[""],"description":""}}}"""
      json.noSpaces should be(expectedJson)
    }(encoder)
  }

  it should "correctly decode a classification project" in {
    val projectId = "test-project-encode"
    val projectName = "test project"

    val projectJson =
      s"""{"id":"$projectId","name":"$projectName","problem":"classification","algorithms":[],"policy":{"class":"NoAlgorithm"},"configuration":{"features":[{"name":"","type":"","description":""}],"labels":{"type":"oneOf","oneOf":[""],"description":""}}}"""

    testDecoder[Project](projectJson) {
      case project: ClassificationProject =>
        assert(project.id == projectId)
        assert(project.name == projectName)
        assert(project.policy == NoAlgorithm())
      case _: RegressionProject =>
        fail()
    }(decoder)
  }
}