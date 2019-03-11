package com.foundaml.server.models

import java.util.UUID

sealed trait ProblemType

case class Classification() extends ProblemType
case class Regression() extends ProblemType
