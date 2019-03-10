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

    implicit val problemTypeGet: Get[ProblemType] = Get[String].map(ProblemTypeSerializer.fromString)
    implicit val problemTypePut: Put[ProblemType] = Put[String].contramap(ProblemTypeSerializer.toString)

    implicit val algorithmPolicyTypeGet: Get[AlgorithmPolicy] = Get[String].map(AlgorithmPolicySerializer.fromString)
    implicit val algorithmPolicyTypePut: Put[AlgorithmPolicy] = Put[String].contramap(AlgorithmPolicySerializer.toString)


  def insert(project: Project) =
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
      ${project.problem.toString},
      ${project.policy.toString},
      ${project.featureType.toString},
      ${project.labelType.toString}
    )""".update

  def read(projectId: String) =
    sql"""
      SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      WHERE id=${projectId}
      """
        .query[(String, String, ProblemType, AlgorithmPolicy, String, String)]

  def readAll() =
    sql"""
        SELECT id, name, problem, algorithm_policy, feature_class, label_class
      FROM projects
      """
      .query[(String, String, ProblemType, AlgorithmPolicy, String, String)]

}
