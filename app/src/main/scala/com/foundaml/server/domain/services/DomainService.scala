package com.foundaml.server.domain.services

import cats.effect.IO
import cats.implicits._
import com.foundaml.server.infrastructure.logging.IOLogging

import com.foundaml.server.infrastructure.logging.IOLogging
import com.foundaml.server.domain.repositories.DomainRepository
import com.foundaml.server.domain.models.DomainClass

class DomainService(domainRepository: DomainRepository) extends IOLogging {

  def readDomain = domainRepository.readAll
  def createDomainModel(domainClass: DomainClass) =
    domainRepository.insert(domainClass)
}
