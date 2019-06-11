package com.hyperplan.domain.models

sealed trait ProblemType {
  def problemType: String
}

case object Classification extends ProblemType {
  val problemType: String = "classification"
}

case object Regression extends ProblemType {
  val problemType: String = "regression"
}
