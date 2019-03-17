package com.foundaml.server.application

import cats.effect
import cats.effect.Timer
import com.foundaml.server.domain.factories.ProjectFactory
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.{App, IO, Task, ZIO}
import scalaz.zio.interop.catz._

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.{Left, Right}
import com.foundaml.server.infrastructure.serialization.PredictionSerializer
import com.foundaml.server.infrastructure.storage.PostgresqlService
import com.foundaml.server.domain.repositories.{AlgorithmsRepository, ProjectsRepository}
import com.foundaml.server.domain.services.PredictionsService
import com.foundaml.server.domain.models.Prediction
import com.foundaml.server.infrastructure.streaming.KinesisService
import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext

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

  def databaseConnected(implicit xa: doobie.Transactor[Task]) =
    for {
      _ <- printLine("Connected to database")
      _ <- printLine("Running SQL scripts")
      _ <- PostgresqlService.initSchema
      _ <- printLine("SQL scripts have been runned successfully")
      projectsRepository = new ProjectsRepository
      algorithmsRepository = new AlgorithmsRepository
      projectFactory = new ProjectFactory(projectsRepository, algorithmsRepository)
      kinesisService <- KinesisService("us-east-2")
      clientStream <- Http1Client
        .stream[Task](BlazeClientConfig.defaultConfig)
        .compile
        .last
      predictionsService = clientStream.fold(
        throw new RuntimeException("Could not instantiate http client")
      )(httpClient => new PredictionsService(projectsRepository, httpClient))
      _ <- printLine("Services have been correctly instanciated")
      predictionId = "test-id"
      _ <- kinesisService.put(
        Prediction(predictionId),
        "test",
        predictionId
      )(PredictionSerializer.encoder)
      _ <- Server
        .stream(
          predictionsService,
          projectsRepository,
          algorithmsRepository,
          projectFactory
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
            case Right(ret) =>
              databaseConnected
            case Left(err) =>
              printLine(s"Could not connect to the database: $err")
          }
        }
      }
    } yield ()

  def printLine(whatToPrint: String) =
    IO(println(whatToPrint))

}
