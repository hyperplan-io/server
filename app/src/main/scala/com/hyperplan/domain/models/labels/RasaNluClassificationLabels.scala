package com.hyperplan.domain.models.labels

case class RasaNluPrediction(
    intent: String,
    predicted: String,
    text: String,
    confidence: Float
) {
  override def equals(o: Any): Boolean = o match {
    case that: RasaNluPrediction =>
      that.intent.equalsIgnoreCase(this.intent)
    case _ => false
  }
  override def hashCode: Int = intent.toUpperCase.hashCode
}

case class RasaNluClassificationLabels(
    report: String,
    accuracy: Float,
    f1Score: Float,
    precision: Float,
    predictions: List[RasaNluPrediction]
)
