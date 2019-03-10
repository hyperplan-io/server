package com.foundaml.server.models.features

sealed trait Features

case class OneToMany(
    key: String,
    values: List[Double]
)

case class TensorFlowClassificationFeatures(
    signatureName: String,
    instances: List[OneToMany]
) extends Features
