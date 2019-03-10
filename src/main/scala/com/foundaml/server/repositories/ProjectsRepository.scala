package com.foundaml.server.repositories

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import io.circe.generic.extras.Configuration, io.circe.generic.extras.auto._

import com.foundaml.server.models._
import com.foundaml.server.models.backends._

import com.foundaml.server.services.infrastructure.serialization._

class ProjectsRepository(implicit xa: Transactor[Task]) {

    implicit val discriminator: Configuration = CirceEncoders.discriminator 

    implicit val problemTypeGet: Get[Either[io.circe.Error,ProblemType]] = Get[String].map(ProblemTypeSerializer.fromString)
    implicit val problemTypePut: Put[ProblemType] = Put[String].contramap(ProblemTypeSerializer.toString)

    implicit val algorithmPolicyTypeGet: Get[Either[io.circe.Error, AlgorithmPolicy]] = Get[String].map(AlgorithmPolicySerializer.fromString)
    implicit val algorithmPolicyTypePut: Put[AlgorithmPolicy] = Put[String].contramap(AlgorithmPolicySerializer.toString)


  def insertQuery(project: Project) =
    sql"""INSERT INTO projects(
      id, 
      name, 
      problem, 
      algorithm_policy, 
      feature_class, 
      label_class
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.problem},
      ${project.policy},
      ${project.featureType.toString},
      ${project.labelType.toString}
    )""".update
    
  def insert(project: Project) = insertQuery(project: Project).run.transact(xa)

  def readQuery(projectId: String) =
    sql"""
      SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      WHERE id=${projectId}
      """
        .query[(String, String, Either[io.circe.Error,ProblemType], Either[io.circe.Error, AlgorithmPolicy], String, String)]

  def read(projectId: String) = readQuery(projectId).unique.transact(xa)

  def readAll() =
    sql"""
        SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      """
      .query[(String, String, Either[io.circe.Error,ProblemType], Either[io.circe.Error, AlgorithmPolicy], String, String)]

}
