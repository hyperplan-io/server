package com.foundaml.server.domain.services

import cats.effect.IO
import cats.implicits._

import com.foundaml.server.domain.repositories.{
  PredictionsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.logging.IOLogging

class PrivacyService(predictionsRepository: PredictionsRepository)
    extends IOLogging {

  def forgetPredictionsLinkedToEntity(name: String, id: String): IO[Int] =
    predictionsRepository.transact(
      predictionsRepository.deletePredictionsLinkedToEntity(name, id)
    )
}
