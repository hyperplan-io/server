package com.foundaml.server.domain.services

import cats.effect.IO
import cats.implicits._
import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.domain.repositories.DomainRepository
import com.foundaml.server.domain.models._

class DomainService(domainRepository: DomainRepository) extends IOLogging {

  def readAllFeatures = domainRepository.readAllFeatures
  def readFeatures(id: String) = domainRepository.readFeatures(id)
  def readAllLabels = domainRepository.readAllLabels
  def readLabels(id: String) = domainRepository.readLabels(id)

  def createFeatures(features: FeaturesConfiguration) =
    domainRepository.insertFeatures(features)
  def createLabels(labels: LabelsConfiguration) =
    domainRepository.insertLabels(labels)

}
