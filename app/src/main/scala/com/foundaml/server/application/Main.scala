package com.foundaml.server.application
import cats.effect.Timer
import com.foundaml.server.domain.FoundaMLConfig
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.domain.services._
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.storage.PostgresqlService
import com.foundaml.server.infrastructure.streaming.{
  KinesisService,
  PubSubService
}
import cats.implicits._
import cats.effect._

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.{Left, Right}
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext.Implicits.global
import com.foundaml.server.domain.repositories.DomainRepository

import cats.effect.ContextShift
import com.foundaml.server.infrastructure.streaming.KafkaService
import com.foundaml.server.infrastructure.metrics.KamonSystemMonitorService
import com.foundaml.server.infrastructure.metrics.PrometheusService

object Main extends IOApp with IOLogging {

  override def main(args: Array[String]): Unit =
    run(args.toList).runAsync(_ => IO(())).unsafeRunSync()

  import cats.effect.ExitCode
  override def run(args: List[String]): IO[ExitCode] =
    loadConfigAndStart().attempt.flatMap(
      _.fold(
        err => logger.error(err.getMessage) *> IO.pure(ExitCode.Error),
        res => IO.pure(ExitCode.Success)
      )
    )

  def databaseConnected(
      config: FoundaMLConfig
  )(implicit xa: doobie.Transactor[IO]) =
    for {
      _ <- logger.info("Connected to database")
      _ <- logger.debug("Running SQL scripts")
      _ <- PrometheusService.monitor
      //_ <- KamonSystemMonitorService.start
      _ <- PostgresqlService.initSchema
      _ <- logger.debug("SQL scripts have been runned successfully")
      projectsRepository = new ProjectsRepository
      algorithmsRepository = new AlgorithmsRepository
      predictionsRepository = new PredictionsRepository
      domainRepository = new DomainRepository
      projectFactory = new ProjectFactory(
        projectsRepository,
        algorithmsRepository
      )
      _ = logger.info("Starting GCP Pubsub service")
      pubSubService <- if (config.gcp.pubsub.enabled) {
        logger.info("Starting GCP PubSub service") *> PubSubService(
          config.gcp.projectId,
          config.gcp.pubsub.predictionsTopicId
        ).map(Some(_))
      } else {
        IO.pure(None)
      }
      kinesisService <- KinesisService("us-east-2")
      kafkaService <- if (config.kafka.enabled) {
        KafkaService(config.kafka.topic, config.kafka.bootstrapServers)
          .map(Some(_))
      } else {
        IO.pure(None)
      }
      predictionsService = new PredictionsService(
        projectsRepository,
        predictionsRepository,
        kinesisService,
        pubSubService,
        kafkaService,
        projectFactory,
        config
      )
      domainService = new DomainService(
        domainRepository
      )
      projectsService = new ProjectsService(
        projectsRepository,
        domainService,
        projectFactory
      )
      algorithmsService = new AlgorithmsService(
        projectsService,
        algorithmsRepository,
        projectsRepository,
        projectFactory
      )

      port = 8080
      _ <- logger.info("Services have been correctly instantiated")
      _ <- logger.info(s"Starting http server on port $port")
      _ <- Server
        .stream(
          predictionsService,
          projectsService,
          algorithmsService,
          domainService,
          kafkaService,
          projectsRepository,
          port
        )
        .compile
        .drain
    } yield ()

  def loadConfigAndStart() =
    pureconfig
      .loadConfig[FoundaMLConfig]
      .fold(
        err => logger.error(s"Failed to load configuration because $err"),
        config => program(config)
      )

  def program(config: FoundaMLConfig) =
    for {
      _ <- logger.info("Starting Foundaml server")
      _ <- logger.info("Connecting to database")
      transactor = PostgresqlService(
        config.database.postgresql.host,
        config.database.postgresql.port.toString,
        config.database.postgresql.database,
        config.database.postgresql.username,
        config.database.postgresql.password,
        config.database.postgresql.schema
      )
      _ <- transactor.use { implicit xa =>
        PostgresqlService.testConnection.attempt.flatMap {
          case Right(_) =>
            import scala.concurrent.ExecutionContext
            databaseConnected(config)
          case Left(err) =>
            logger.info(s"Could not connect to the database: $err")
        }
      }
    } yield ()

}
