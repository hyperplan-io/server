package com.hyperplan.application

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

import scalacache._
import scalacache.caffeine._

import pureconfig.generic.auto._

import com.hyperplan.domain.repositories.{
  AlgorithmsRepository,
  DomainRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.hyperplan.domain.services._
import com.hyperplan.infrastructure.auth.AuthenticationService._
import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.infrastructure.metrics.PrometheusService
import com.hyperplan.infrastructure.storage.PostgresqlService
import com.hyperplan.infrastructure.streaming.{
  KafkaService,
  KinesisService,
  PubSubService
}

import scala.util.{Left, Right}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import cats.effect.Resource
import org.http4s.client.Client

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

  import com.hyperplan.infrastructure.auth.JwtAuthenticationService
  import com.hyperplan.domain.models.Project
  def databaseConnected(
      config: ApplicationConfig
  )(implicit xa: doobie.Transactor[IO]) =
    for {
      _ <- logger.info("Connected to database")
      _ <- logger.debug("Running SQL scripts")
      _ <- PrometheusService.monitor
      //_ <- KamonSystemMonitorService.start
      _ <- PostgresqlService.initSchema
      _ <- logger.debug("SQL scripts have been runned successfully")
      _ <- if (config.security.protectPredictionRoute) {
        logger.info("prediction route is token protected")
      } else {
        logger.info("prediction route is not protected")
      }
      projectCache: Cache[Project] = CaffeineCache[Project]
      projectsRepository = new ProjectsRepository
      algorithmsRepository = new AlgorithmsRepository
      predictionsRepository = new PredictionsRepository
      domainRepository = new DomainRepository
      blazeClient: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](
        ExecutionContext.global
      ).resource
      implicit0(actorSystem: ActorSystem) <- IO(ActorSystem())
      implicit0(actorMaterializer: ActorMaterializer) <- IO(ActorMaterializer())
      pubSubService <- if (config.gcp.pubsub.enabled) {
        logger.info("Starting GCP PubSub service") *> PubSubService(
          config.gcp.projectId,
          config.gcp.privateKey,
          config.gcp.clientEmail
        ).map(Some(_))
      } else {
        IO.pure(None)
      }
      kinesisService <- if (config.kinesis.enabled) {
        logger.info(s"Starting Kinesis service") *> KinesisService(
          config.kinesis.region
        ).map(_.some)
      } else IO.pure(None)
      kafkaService <- if (config.kafka.enabled) {
        logger.info(
          s"Starting Kafka service with bootstrap servers ${config.kafka.bootstrapServers}"
        ) *> KafkaService(config.kafka.topic, config.kafka.bootstrapServers)
          .map(Some(_))
      } else {
        IO.pure(None)
      }
      domainService = new DomainService(
        domainRepository
      )
      backendService = new BackendService(blazeClient)
      projectsService = new ProjectsService(
        projectsRepository,
        algorithmsRepository,
        domainService,
        backendService,
        projectCache
      )
      predictionsService = new PredictionsService(
        predictionsRepository,
        projectsService,
        backendService,
        kinesisService,
        pubSubService,
        kafkaService,
        config
      )
      privacyService = new PrivacyService(predictionsRepository)
      port = 8080
      _ <- logger.info("Services have been correctly instantiated")
      _ <- logger.info(s"Starting http server on port $port")
      publicKeyRaw = config.encryption.publicKey
      privateKeyRaw = config.encryption.privateKey
      publicKey <- publicKeyRaw.fold[IO[Option[PublicKey]]](IO.pure(None))(
        publicKey => JwtAuthenticationService.publicKey(publicKey).map(_.some)
      )
      privateKey <- privateKeyRaw.fold[IO[Option[PrivateKey]]](IO.pure(None))(
        privateKey =>
          JwtAuthenticationService.privateKey(privateKey).map(_.some)
      )
      _ <- logger.info("encryption keys initialized")
      _ <- {
        implicit val publicKeyImplicit = publicKey
        implicit val privateKeyImplicit = privateKey
        implicit val secret = config.encryption.secret
        implicit val configImplicit = config
        Server
          .stream(
            predictionsService,
            projectsService,
            domainService,
            privacyService,
            kafkaService,
            projectsRepository,
            port
          )
          .fold(
            err => IO.raiseError(err),
            _.compile.drain
          )
      }
    } yield ()

  def loadConfigAndStart() =
    pureconfig
      .loadConfig[ApplicationConfig]
      .fold(
        err => logger.error(s"Failed to load configuration because $err"),
        config => program(config)
      )

  def program(config: ApplicationConfig) =
    for {
      _ <- logger.info("Starting Hyperplan server")
      _ <- logger.info("Connecting to database")
      transactor = PostgresqlService(
        config.database.postgresql.host,
        config.database.postgresql.port.toString,
        config.database.postgresql.database,
        config.database.postgresql.username,
        config.database.postgresql.password,
        config.database.postgresql.schema,
        config.database.postgresql.threadPool
      )
      _ <- transactor.use { implicit xa =>
        PostgresqlService.testConnection.attempt.flatMap {
          case Right(_) =>
            databaseConnected(config)
          case Left(err) =>
            logger.info(s"Could not connect to the database: $err")
        }
      }
    } yield ()

}
