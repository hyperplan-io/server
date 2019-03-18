package com.foundaml.server.application

import cats.effect
import cats.effect.Timer
import com.foundaml.server.domain.FoundaMLConfig
import com.foundaml.server.domain.factories.ProjectFactory
import com.foundaml.server.domain.repositories.{AlgorithmsRepository, PredictionsRepository, ProjectsRepository}
import com.foundaml.server.domain.services.{PredictionsService, ProjectsService}
import com.foundaml.server.infrastructure.storage.PostgresqlService
import com.foundaml.server.infrastructure.streaming.KinesisService
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.interop.catz._
import scalaz.zio.{App, IO, Task, ZIO}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.{Left, Right}

import pureconfig.generic.auto._

object Main extends App {

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
    program().either.map(_.fold(err => {
      println(err)
      1
    }, _ => 0))

  def databaseConnected(
      config: FoundaMLConfig
  )(implicit xa: doobie.Transactor[Task]) =
    for {
      _ <- printLine("Connected to database")
      _ <- printLine("Running SQL scripts")
      _ <- PostgresqlService.initSchema
      _ <- printLine("SQL scripts have been runned successfully")
      projectsRepository = new ProjectsRepository
      algorithmsRepository = new AlgorithmsRepository
      predictionsRepository = new PredictionsRepository
      projectFactory = new ProjectFactory(
        projectsRepository,
        algorithmsRepository
      )
      kinesisService <- KinesisService("us-east-2")
      predictionsService = new PredictionsService(
        projectsRepository,
        predictionsRepository,
        kinesisService,
        projectFactory,
        config
      )
      projectsService = new ProjectsService(
        projectsRepository,
        projectFactory
      )
      _ <- printLine("Services have been correctly instantiated")
      _ <- Server
        .stream(
          predictionsService,
          projectsService,
          projectsRepository,
          algorithmsRepository,
          projectFactory,
        )
        .compile
        .drain
    } yield ()

  def program(): Task[Unit] =
    for {
      _ <- printLine("Starting Foundaml server")
      _ <- printLine("Connecting to database")
      transactor = PostgresqlService(
        "127.0.0.1",
        "5432",
        "postgres",
        "postgres",
        "postgres"
      )
      _ <- transactor.use { implicit xa =>
        PostgresqlService.testConnection.flatMap {
          _.toEither match {
            case Right(_) =>
              pureconfig
                .loadConfig[FoundaMLConfig]
                .fold(
                  err =>
                    printLine(s"Failed to load configuration because $err"),
                  config => databaseConnected(config)
                )

            case Left(err) =>
              printLine(s"Could not connect to the database: $err")
          }
        }
      }
    } yield ()

  def printLine(whatToPrint: String) =
    IO(println(whatToPrint))

}
