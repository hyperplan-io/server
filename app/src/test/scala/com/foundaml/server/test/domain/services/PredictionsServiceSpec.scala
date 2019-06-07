package com.foundaml.server.test.domain.services

import java.util.UUID

import com.foundaml.server.domain._
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.FeaturesValidationFailed
import com.foundaml.server.domain.services.PredictionsService
import org.scalatest._
import org.scalatest.Inside.inside
import com.foundaml.server.domain.models.features._
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  PredictionsRepository,
  ProjectsRepository,
  DomainRepository
}
import com.foundaml.server.domain.services.{ProjectsService, DomainService}
import com.foundaml.server.infrastructure.streaming.{
  KinesisService,
  PubSubService,
  KafkaService
}
import com.foundaml.server.test.{
  AlgorithmGenerator,
  ProjectGenerator,
  TestDatabase
}
import cats.implicits._
import cats.effect.{IO, Timer}
import scala.concurrent.ExecutionContext

import scala.util.Try

class PredictionsServiceSpec extends FlatSpec with TestDatabase {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  val config = FoundaMLConfig(
    KinesisConfig(enabled = false, "predictionsStream", "examplesStream"),
    GCPConfig(
      "myProjectId",
      PubSubConfig(
        enabled = false,
        "myTopic"
      )
    ),
    KafkaConfig(
      enabled = false,
      topic = "exampleTopic",
      bootstrapServers = "localhost:9092"
    ),
    DatabaseConfig(
      PostgreSqlConfig(
        "host",
        5432,
        "database",
        "username",
        "password",
        "public"
      )
    ),
    EncryptionConfig(
      "",
      ""
    ),
    AdminCredentials(
      "username",
      "password"
    )
  )

  val projectsRepository = new ProjectsRepository()(xa)
  val algorithmsRepository = new AlgorithmsRepository()(xa)
  val predictionsRepository = new PredictionsRepository()(xa)
  val domainRepository = new DomainRepository()(xa)

  val kinesisService: KinesisService =
    KinesisService("fake-region").unsafeRunSync()
  val pubSubService: PubSubService =
    PubSubService("myProjectId", "myTopic").unsafeRunSync()
  val kafkaService: KafkaService =
    KafkaService(config.kafka.topic, config.kafka.bootstrapServers)
      .unsafeRunSync()
  val domainService = new DomainService(
    domainRepository
  )
  val projectsService = new ProjectsService(
    projectsRepository,
    domainService
  )

  val predictionsService: PredictionsService =
    new PredictionsService(
      predictionsRepository,
      projectsService,
      kinesisService,
      Some(pubSubService),
      Some(kafkaService),
      config
    )
}
