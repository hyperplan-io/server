package com.hyperplan.domain.models.labels

case class BasicLabel(label: String, probability: Float)
object BasicLabel {
  type BasicLabels = List[BasicLabel]
}
