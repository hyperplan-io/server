package com.foundaml.server.models

import java.util.UUID

sealed trait ProblemType

case object Classification extends ProblemType
case object Regression extends ProblemType
