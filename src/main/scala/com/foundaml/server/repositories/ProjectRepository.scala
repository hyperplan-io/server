package com.foundaml.server.repositories

import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import com.foundaml.server.models.Project

class ProjectsRepository(implicit xa: HikariTransactor[Task]){

  def insert(project: Project) = 
    sql"""INSERT INTO projects(
      id, 
      name, 
      problem, 
      policy, 
      feature_type, 
      label_type
    ) VALUES(
      ${project.id},
      ${project.name},
      ${project.problem.toString},
      ${project.policy.toString},
      ${project.featureType.toString},
      ${project.labelType.toString},
    )""".update

}
