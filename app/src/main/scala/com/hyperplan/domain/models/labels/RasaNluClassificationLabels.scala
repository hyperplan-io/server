package com.hyperplan.domain.models.labels

case class RasaNluIntent(
    name: String,
    confidence: Float
) {
  override def equals(o: Any): Boolean = o match {
    case that: RasaNluIntent =>
      that.name.equalsIgnoreCase(this.name)
    case _ => false
  }
  override def hashCode: Int = name.toUpperCase.hashCode
}

case class RasaNluClassificationLabels(
    intent: RasaNluIntent,
    intent_ranking: List[RasaNluIntent]
)
