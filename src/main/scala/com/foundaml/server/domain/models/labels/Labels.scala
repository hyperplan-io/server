package com.foundaml.server.domain.models.labels

case class Labels(labels: Set[Label])

sealed trait Label {
  def id: String
  def label: String
  def correctExampleUrl: String
  def incorrectExampleUrl: String
}

case class ClassificationLabel(id: String, label: String, probability: Float, correctExampleUrl: String, incorrectExampleUrl: String) extends Label
