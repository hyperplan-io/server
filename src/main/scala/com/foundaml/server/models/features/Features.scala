package com.foundaml.server.models.features

sealed trait Features

case class OneToMany[K,V](
  key: K,
  values: List[V]
)

case class TensorFlowClassificationFeatures(
  signatureName: String,
  instances: List[OneToMany[String, Double]]
) extends Features
