package com.hyperplan.infrastructure.metrics

import kamon.system.SystemMetrics
import cats.effect.IO

trait SystemMonitorService {
  def start: IO[Unit]
  def stop: IO[Unit]
}

object KamonSystemMonitorService extends SystemMonitorService {
  def start = IO(SystemMetrics.startCollecting())
  def stop = IO(SystemMetrics.stopCollecting())
}
