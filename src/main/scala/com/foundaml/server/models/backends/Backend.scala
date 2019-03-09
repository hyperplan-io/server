package com.foundaml.server.models.backends


import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._

sealed trait Backend

case class Local(
  compute: Features => Labels 
) extends Backend
case class TensorFlowBackend(host: String, port: String) extends Backend
