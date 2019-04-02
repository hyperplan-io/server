package com.foundaml.server.application

import cats.effect
import cats.effect.Timer
import com.foundaml.server.domain.FoundaMLConfig
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.domain.services.{
  AlgorithmsService,
  PredictionsService,
  ProjectsService
}
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.infrastructure.storage.PostgresqlService
import com.foundaml.server.infrastructure.streaming.{
  KinesisService,
  PubSubService
}
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.interop.catz._
import scalaz.zio.{App, IO, Task, ZIO}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.{Left, Right}
import pureconfig.generic.auto._

object Main extends App with IOLogging {

  implicit val timer: Timer[Task] = new Timer[Task] {
    val zioClock = Clock.Live.clock

    override def clock: effect.Clock[Task] = new effect.Clock[Task] {
      override def realTime(unit: TimeUnit) =
        zioClock.nanoTime.map(unit.convert(_, NANOSECONDS))

      override def monotonic(unit: TimeUnit) = zioClock.currentTime(unit)
    }

    override def sleep(duration: FiniteDuration): Task[Unit] =
      zioClock.sleep(Duration.fromScala(duration))
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    loadConfigAndStart()
      .fold(
        err => errorLog(err.getMessage) *> Task.fail(err),
        res => Task.succeed(res)
      )
      .either
      .map(_.fold(_ => {
        1
      }, _ => 0))

  def databaseConnected(
      config: FoundaMLConfig
  )(implicit xa: doobie.Transactor[Task]) =
    for {
      _ <- infoLog("Connected to database")
      _ <- debugLog("Running SQL scripts")
      _ <- PostgresqlService.initSchema
      _ <- debugLog("SQL scripts have been runned successfully")
      projectsRepository = new ProjectsRepository
      algorithmsRepository = new AlgorithmsRepository
      predictionsRepository = new PredictionsRepository
      projectFactory = new ProjectFactory(
        projectsRepository,
        algorithmsRepository
      )
      _ = logger.info("Starting GCP Pubsub service")
      pubSubService <- if (config.gcp.pubsub.enabled) {
        infoLog("Starting GCP PubSub service") *> PubSubService(
          config.gcp.projectId,
          config.gcp.pubsub.predictionsTopicId
        ).map(Some(_))
      } else {
        Task.succeed(None)
      }
      kinesisService <- KinesisService("us-east-2")
      predictionsService = new PredictionsService(
        projectsRepository,
        predictionsRepository,
        kinesisService,
        pubSubService,
        projectFactory,
        config
      )
      projectsService = new ProjectsService(
        projectsRepository,
        projectFactory
      )
      algorithmsService = new AlgorithmsService(
        algorithmsRepository,
        projectsRepository,
        projectFactory
      )
      port = 8080
      _ <- infoLog("Services have been correctly instantiated")
      _ <- infoLog(s"Starting http server on port $port")
      _ <- Server
        .stream(
          predictionsService,
          projectsService,
          algorithmsService,
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
        err => errorLog(s"Failed to load configuration because $err"),
        config => program(config)
      )

  def program(config: FoundaMLConfig): Task[Unit] =
    for {
      _ <- infoLog("Starting Foundaml server")
      _ <- infoLog("Connecting to database")
      transactor = PostgresqlService(
        config.database.postgresql.host,
        config.database.postgresql.port.toString,
        config.database.postgresql.database,
        config.database.postgresql.username,
        config.database.postgresql.password
      )
      _ <- transactor.use { implicit xa =>
        PostgresqlService.testConnection.flatMap {
          _.toEither match {
            case Right(_) =>
              databaseConnected(config)
            case Left(err) =>
              infoLog(s"Could not connect to the database: $err")
          }
        }
      }
    } yield ()

}
