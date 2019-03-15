package com.foundaml.server.domain.models.labels

case class Labels(labels: Set[Label])

sealed trait Label

case class ClassificationLabel(label: String, probability: Float) extends Label
