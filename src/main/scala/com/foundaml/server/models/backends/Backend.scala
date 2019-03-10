package com.foundaml.server.models.backends

import com.foundaml.server.models.features._
import com.foundaml.server.models.labels._

import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.JsonCodec
import io.circe.parser.decode, io.circe.syntax._
import io.circe.generic.extras.Configuration, io.circe.generic.extras.auto._

sealed trait Backend

case class Local(
    computed: Labels
) extends Backend

case class TensorFlowBackend(host: String, port: String) extends Backend

