package com.hyperplan.domain.models

sealed trait Protocol

case object Http extends Protocol
case object Grpc extends Protocol
case object LocalCompute extends Protocol
