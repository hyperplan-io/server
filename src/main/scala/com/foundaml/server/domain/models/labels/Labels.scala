package com.foundaml.server.domain.models.labels

case class Labels(labels: Set[Label])

sealed trait Label {
  def id: String
  def label: String
}

case class ClassificationLabel(id: String, label: String, probability: Float) extends Label
