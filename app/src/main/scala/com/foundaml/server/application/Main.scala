package com.foundaml.server.application
import cats.effect.Timer
import com.foundaml.server.domain.FoundaMLConfig
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
import cats.effect.ExitCode

object Main extends IOApp with IOLogging {

  override def main(args: Array[String]): Unit =
    run(args.toList).runAsync(_ => IO(())).unsafeRunSync()

  import kamon.Kamon
  def killAll: IO[Unit] =
    IO.fromFuture(IO(Kamon.stopAllReporters()))

  override def run(args: List[String]): IO[ExitCode] =
    loadConfigAndStart().attempt.flatMap(
      _.fold(
        err => killAll *> logger.error(err.getMessage).as(ExitCode.Error),
        res => IO.pure(ExitCode.Success)
      )
    )

  import com.foundaml.server.infrastructure.auth.JwtAuthenticationService
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
      domainService = new DomainService(
        domainRepository
      )
      projectsService = new ProjectsService(
        projectsRepository,
        domainService
      )
      predictionsService = new PredictionsService(
        predictionsRepository,
        projectsService,
        kinesisService,
        pubSubService,
        kafkaService,
        config
      )
      algorithmsService = new AlgorithmsService(
        projectsService,
        algorithmsRepository,
        projectsRepository
      )

      port = 8080
      _ <- logger.info("Services have been correctly instantiated")
      _ <- logger.info(s"Starting http server on port $port")
      publicKeyRaw = config.encryption.publicKey
      privateKeyRaw = config.encryption.privateKey
      publicKey <- JwtAuthenticationService.publicKey(publicKeyRaw)
      privateKey <- JwtAuthenticationService.privateKey(privateKeyRaw)
      _ <- logger.info("encryption keys initialized")
      _ <- {
        implicit val publicKeyImplicit = publicKey
        implicit val privateKeyImplicit = privateKey
        implicit val configImplicit = config
        Server
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
      }
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
