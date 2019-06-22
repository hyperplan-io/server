package com.hyperplan.test.domain.services

import cats.effect.{IO, Timer}

import scalacache.Cache
import com.hyperplan.domain.models.Project
import scalacache.caffeine.CaffeineCache

import com.hyperplan.application._
import com.hyperplan.domain.repositories.{
  AlgorithmsRepository,
  DomainRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.hyperplan.domain.services.{
  DomainService,
  PredictionsService,
  ProjectsService
}
import com.hyperplan.infrastructure.streaming.{
  KafkaService,
  KinesisService,
  PubSubService
}
import com.hyperplan.test.TestDatabase
import com.hyperplan.test.TestDatabase
import org.scalatest._

import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.concurrent.ExecutionContext

class PredictionsServiceSpec() extends FlatSpec with TestDatabase with BeforeAndAfterAll {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  val config = ApplicationConfig(
    KinesisConfig(enabled = false, "predictionsStream", "examplesStream", "test-region"),
    GCPConfig(
      "myProjectId",
      "privateKey",
      "clientEmail",
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
        "public",
        2
      )
    ),
    EncryptionConfig(
      Some(""),
      Some(""),
      None,
      "hyperplan-test"
    ),
    AdminCredentials(
      "username",
      "password"
    ),
    PredictionConfig(
      true
    ),
    SecurityConfig(
      true
    )
  )

  val projectsRepository = new ProjectsRepository()(xa)
  val algorithmsRepository = new AlgorithmsRepository()(xa)
  val predictionsRepository = new PredictionsRepository()(xa)
  val domainRepository = new DomainRepository()(xa)

  val kinesisService: KinesisService =
    KinesisService("fake-region").unsafeRunSync()
  val pubSubService: PubSubService =
    PubSubService("myProjectId", "privateKey", "clientEmail").unsafeRunSync()
  val kafkaService: KafkaService =
    KafkaService(config.kafka.topic, config.kafka.bootstrapServers)
      .unsafeRunSync()
  val domainService = new DomainService(
    domainRepository
  )

  val projectCache: Cache[Project] = CaffeineCache[Project]
  val projectsService = new ProjectsService(
    projectsRepository,
    domainService,
    projectCache
  )

  val predictionsService: PredictionsService =
    new PredictionsService(
      predictionsRepository,
      projectsService,
      Some(kinesisService),
      Some(pubSubService),
      Some(kafkaService),
      config
    )
}
