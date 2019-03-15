package com.foundaml.server.domain.models.labels


case class Labels(labels: List[Label])

sealed trait Label

case class ClassificationLabel(label: String, probability: Float) extends Label