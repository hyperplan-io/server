package com.hyperplan.domain.models.features

sealed trait BasicHttpFeature
case class BasicHttpIntFeature(name: String, value: Int)
    extends BasicHttpFeature
case class BasicHttpFloatFeature(name: String, value: Float)
    extends BasicHttpFeature
case class BasicHttpStringFeature(name: String, value: String)
    extends BasicHttpFeature
case class BasicHttpIntArrayFeature(name: String, value: List[Int])
    extends BasicHttpFeature
case class BasicHttpFloatArrayFeature(name: String, value: List[Float])
    extends BasicHttpFeature
case class BasicHttpStringArrayFeature(name: String, value: List[String])
    extends BasicHttpFeature

case class BasicHttpAPIFeatures(features: List[BasicHttpFeature])
