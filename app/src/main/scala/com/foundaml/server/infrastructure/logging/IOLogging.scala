package com.foundaml.server.infrastructure.logging

import cats.effect.IO
import org.slf4j.LoggerFactory

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

  implicit val logger: Logger = new Logger {

    @transient
    private lazy val logger = com.typesafe.scalalogging
      .Logger(LoggerFactory.getLogger(getClass.getName))

    def trace(message: String): IO[Unit] = IO(logger.trace(message))
    def trace(message: String, cause: Throwable): IO[Unit] =
      IO(logger.trace(message, cause))
    def debug(message: String): IO[Unit] = IO(logger.debug(message))
    def debug(message: String, cause: Throwable): IO[Unit] =
      IO(logger.debug(message, cause))
    def info(message: String): IO[Unit] = IO(logger.info(message))
    def info(message: String, cause: Throwable): IO[Unit] =
      IO(logger.info(message, cause))
    def warn(message: String): IO[Unit] = IO(logger.warn(message))
    def warn(message: String, cause: Throwable): IO[Unit] =
      IO(logger.warn(message, cause))
    def error(message: String): IO[Unit] = IO(logger.error(message))
    def error(message: String, cause: Throwable): IO[Unit] =
      IO(logger.error(message, cause))
  }
}
