package com.foundaml.server.domain.services

import cats.effect.IO
import cats.implicits._
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.domain.repositories.DomainRepository
import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors._

import doobie.util.invariant.UnexpectedEnd

class DomainService(domainRepository: DomainRepository) extends IOLogging {

  def readAllFeatures =
    domainRepository.readAllFeatures

  def readFeatures(id: String) =
    domainRepository.readFeatures(id).handleErrorWith {
      case UnexpectedEnd =>
        IO.raiseError(FeaturesClassDoesNotExist(id))
    }

  def readAllLabels = domainRepository.readAllLabels
  def readLabels(id: String) =
    domainRepository.readLabels(id).handleErrorWith {
      case UnexpectedEnd =>
        IO.raiseError(LabelsClassDoesNotExist(id))
    }

  def createFeatures(features: FeaturesConfiguration) =
    domainRepository
      .insertFeatures(features)
      .flatMap(_.fold(err => IO.raiseError(err), result => IO.pure(result)))
  def createLabels(labels: LabelsConfiguration) =
    domainRepository
      .insertLabels(labels)
      .flatMap(_.fold(err => IO.raiseError(err), result => IO.pure(result)))

}
