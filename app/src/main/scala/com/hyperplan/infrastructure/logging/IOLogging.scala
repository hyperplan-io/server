package com.hyperplan.infrastructure.logging

import cats.effect.IO
import cats.implicits._

import org.slf4j.LoggerFactory
import cats.data.Kleisli

trait IOLogging {

  trait Logger {
    def trace(message: String): IO[Unit]
    def trace(message: String, cause: Throwable): IO[Unit]
    def debug(message: String): IO[Unit]
    def debug(message: String, cause: Throwable): IO[Unit]
    def info(message: String): IO[Unit]
    def info(message: String, cause: Throwable): IO[Unit]
    def warn(message: String): IO[Unit]
    def warn(message: String, cause: Throwable): IO[Unit]
    def error(message: String): IO[Unit]
    def error(message: String, cause: Throwable): IO[Unit]
  }

  @transient
  private lazy val scalaLogger = com.typesafe.scalalogging
    .Logger(LoggerFactory.getLogger(getClass.getName))

  implicit val logger: Logger = new Logger {

    def trace(message: String): IO[Unit] = IO(scalaLogger.trace(message))
    def trace(message: String, cause: Throwable): IO[Unit] =
      IO(scalaLogger.trace(message, cause))
    def debug(message: String): IO[Unit] = IO(scalaLogger.debug(message))
    def debug(message: String, cause: Throwable): IO[Unit] =
      IO(scalaLogger.debug(message, cause))
    def info(message: String): IO[Unit] = IO(scalaLogger.info(message))
    def info(message: String, cause: Throwable): IO[Unit] =
      IO(scalaLogger.info(message, cause))
    def warn(message: String): IO[Unit] = IO(scalaLogger.warn(message))
    def warn(message: String, cause: Throwable): IO[Unit] =
      IO(scalaLogger.warn(message, cause))
    def error(message: String): IO[Unit] = IO(scalaLogger.error(message))
    def error(message: String, cause: Throwable): IO[Unit] =
      IO(scalaLogger.error(message, cause))
  }
}
