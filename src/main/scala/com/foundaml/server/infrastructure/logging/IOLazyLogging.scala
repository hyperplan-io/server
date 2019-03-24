package com.foundaml.server.infrastructure.logging

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import scalaz.zio.Task

trait IOLazyLogging {

  protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

  def debugLog(message: String) = Task(logger.debug(message))
  def infoLog(message: String) = Task(logger.info(message))
  def warnLog(message: String) = Task(logger.warn(message))
  def errorLog(message: String) = Task(logger.error(message))
}
