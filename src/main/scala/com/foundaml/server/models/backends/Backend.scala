package com.foundaml.server.models.backends

sealed trait Backend

case class Local[FeatureType, LabelType](
  compute: (FeatureType) =>  LabelType
) extends Backend
case class TensorFlowBackend(host: String, port: String) extends Backend
