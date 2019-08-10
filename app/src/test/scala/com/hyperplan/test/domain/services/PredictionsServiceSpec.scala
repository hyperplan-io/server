package com.hyperplan.test.domain.services

import cats.effect.{IO, Timer}
import scalacache.Cache
import com.hyperplan.domain.models.Project
import scalacache.caffeine.CaffeineCache
import com.hyperplan.application._
import com.hyperplan.domain.repositories.{
  DomainRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.hyperplan.domain.services.{
  BackendServiceLive,
  DomainServiceLive,
  PredictionsServiceLive,
  ProjectsServiceLive
}
import com.hyperplan.infrastructure.streaming.{
  KafkaService,
  KinesisService,
  PubSubService
}
import com.hyperplan.test.TestDatabase
import com.hyperplan.test.TestDatabase
import org.scalatest._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender
import org.http4s.client.blaze.BlazeClientBuilder

class PredictionsServiceSpec()
    extends TestKit(ActorSystem("test"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with TestDatabase
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val mat = ActorMaterializer()
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  val blazeClient = BlazeClientBuilder[IO](
    ExecutionContext.global
  ).resource

  val config = ApplicationConfig(
    KinesisConfig(
      enabled = false,
      "predictionsStream",
      "examplesStream",
      "test-region"
    ),
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
  val predictionsRepository = new PredictionsRepository()(xa)
  val domainRepository = new DomainRepository()(xa)

  val kinesisService: KinesisService =
    KinesisService("fake-region").unsafeRunSync()
  val pubSubService: PubSubService =
    PubSubService("myProjectId", "privateKey", "clientEmail").unsafeRunSync()
  val kafkaService: KafkaService =
    KafkaService(config.kafka.topic, config.kafka.bootstrapServers)
      .unsafeRunSync()
  val domainService = new DomainServiceLive(
    domainRepository
  )
  val backendService = new BackendServiceLive(blazeClient)

  val projectCache: Cache[Project] = CaffeineCache[Project]
  val projectsService = new ProjectsServiceLive(
    projectsRepository,
    domainService,
    backendService,
    projectCache
  )

  val predictionsService: PredictionsServiceLive =
    new PredictionsServiceLive(
      predictionsRepository,
      projectsService,
      backendService,
      Some(kinesisService),
      Some(pubSubService),
      Some(kafkaService),
      config
    )
}
