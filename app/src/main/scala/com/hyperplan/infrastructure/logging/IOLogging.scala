/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

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
