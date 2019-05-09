package com.foundaml.server.infrastructure.metrics

import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import cats.effect.IO

object PrometheusService {
  def monitor = IO(Kamon.addReporter(new PrometheusReporter()))
}
