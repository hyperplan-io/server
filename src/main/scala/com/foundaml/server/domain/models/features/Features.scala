package com.foundaml.server.domain.models.features

case class Features(features: List[Feature])

sealed trait Feature

case class DoubleFeature(value: Double) extends Feature
case class FloatFeature(value: Float) extends Feature
case class IntFeature(value: Int) extends Feature
case class StringFeature(value: String) extends Feature
