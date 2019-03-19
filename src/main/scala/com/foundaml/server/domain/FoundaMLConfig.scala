package com.foundaml.server.domain

case class KinesisConfig(
    enabled: Boolean,
    predictionsStream: String,
    examplesStream: String
)

case class FoundaMLConfig(kinesis: KinesisConfig)
