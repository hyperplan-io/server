package com.foundaml.server

import cats.effect.{Effect}
import cats.effect
import cats.effect.Timer

import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.{Left, Right}

import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.interop.catz.taskEffectInstances
import scalaz.zio.interop.catz._
import scalaz.zio.{IO, App, ZIO, Promise, Task}

import services._
import services.serialization._
import services.streaming._
import services.http._
import services.infrastructure.storage._

import models._

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

  import services.serialization.CirceEncoders._
  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    program().either.map(_.fold(err => {
      println(err)
      1
    }, _ => 0))

  def databaseConnected() =
    for {
      _ <- printLine("Connected to database")
      jsonService = new JsonService()
      kinesisService <- KinesisService("us-east-2", jsonService)
      _ <- printLine("Services have been correctly instanciated")
      predictionId = "test-id"
      _ <- kinesisService.put(
        Prediction(predictionId),
        "test",
        predictionId
      )
      _ <- Server.stream.compile.drain
    } yield ()

  def program(): Task[Unit] =
    for {
      _ <- printLine("Starting Foundaml server")
      _ <- printLine("Connecting to database")
      transactor = PostgresqlService(
        "127.0.0.1",
        "5432",
        "test",
        "postgres",
        "postgres"
      )
      _ <- transactor.use { implicit xa =>
        PostgresqlService.testConnection.flatMap {
          _.toEither match {
            case Right(ret) =>
              databaseConnected()
            case Left(err) =>
              printLine(s"Could not connect to the database: $err")
          }
        }
      }
    } yield ()

  def printLine(whatToPrint: String) =
    IO(println(whatToPrint))

}
