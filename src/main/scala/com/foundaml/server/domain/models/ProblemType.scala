package com.foundaml.server.domain.models

sealed trait ProblemType
object ProblemType {
  val classification = "classification"
}
case class Classification() extends ProblemType
case class Regression() extends ProblemType
