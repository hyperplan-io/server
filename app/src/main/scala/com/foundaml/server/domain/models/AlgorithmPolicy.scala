package com.foundaml.server.domain.models

import scala.util.Random
import cats.implicits._

sealed trait AlgorithmPolicy {
  def take(): Option[String]
  def name: String
}
case class NoAlgorithm() extends AlgorithmPolicy {
  override def take(): Option[String] = None
  val name = NoAlgorithm.name
}

object NoAlgorithm {
  val name = "NoAlgorithm"
}

case class DefaultAlgorithm(algorithmId: String) extends AlgorithmPolicy {
  override def take(): Option[String] = Some(algorithmId)
  val name = DefaultAlgorithm.name
}

object DefaultAlgorithm {
  val name = "DefaultAlgorithm"
}

case class AlgorithmWeight(algorithmId: String, weight: Float)
case class WeightedAlgorithm(weights: List[AlgorithmWeight])
    extends AlgorithmPolicy {

  val cumulativeWeights = weights.foldLeft[List[Float]](Nil) {
    case (acc, tuple) =>
      acc :+ (acc.lastOption.getOrElse(0.0f) + tuple.weight)
  }
  val weightsZipCumulativeWeights = weights.zip(cumulativeWeights)

  override def take(): Option[String] = {
    val rand = Random.nextFloat() * cumulativeWeights.lastOption.getOrElse(0f)
    weightsZipCumulativeWeights
      .foldLeft[List[AlgorithmWeight]](Nil) {
        case (acc, tuple) =>
          val (algorithmWeight, cumWeight) = tuple
          if (cumWeight > rand) {
            acc :+ algorithmWeight
          } else {
            acc
          }
      }
      .headOption
      .map(_.algorithmId)
  }
  val name = WeightedAlgorithm.name
}

object WeightedAlgorithm {
  val name = "WeightedAlgorithm"
}
