package com.foundaml.server.domain.models

sealed trait ProblemType

case class Classification() extends ProblemType
case class Regression() extends ProblemType
