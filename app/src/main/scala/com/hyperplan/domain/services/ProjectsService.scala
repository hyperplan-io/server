package com.hyperplan.domain.services

import cats.data.{EitherT, NonEmptyChain}
import cats.effect.IO

import com.hyperplan.application.controllers.requests.PostProjectRequest
import com.hyperplan.domain.errors.{AlgorithmError, ProjectError}
import com.hyperplan.domain.models.{
  Algorithm,
  AlgorithmPolicy,
  Project,
  SecurityConfiguration
}
import com.hyperplan.domain.models.backends.Backend

trait ProjectsService {
  def createProject(
      projectRequest: PostProjectRequest
  ): EitherT[IO, NonEmptyChain[ProjectError], Project]

  def updateProject(
      projectId: String,
      name: Option[String],
      policy: Option[AlgorithmPolicy]
  ): EitherT[IO, NonEmptyChain[ProjectError], Project]
  def deleteProject(projectId: String): IO[Int]

  def readProjects: IO[List[Project]]
  def readProject(id: String): IO[Option[Project]]
  def deleteAlgorithm(projectId: String, algorithmId: String): IO[Int]
  def createAlgorithm(
      id: String,
      backend: Backend,
      projectId: String,
      security: SecurityConfiguration
  ): EitherT[IO, NonEmptyChain[AlgorithmError], Algorithm]
}
