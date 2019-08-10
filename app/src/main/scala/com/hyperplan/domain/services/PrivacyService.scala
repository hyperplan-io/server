package com.hyperplan.domain.services

import cats.effect.IO

trait PrivacyService {
  def forgetPredictionsLinkedToEntity(name: String, id: String): IO[Int]
}
