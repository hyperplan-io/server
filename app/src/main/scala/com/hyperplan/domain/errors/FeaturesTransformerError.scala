package com.hyperplan.domain.errors

sealed trait RasaFeaturesTransformerError extends Throwable

case class IllegalFieldType(field: String, classType: String)
    extends RasaFeaturesTransformerError
case class DidNotFindField(field: String) extends RasaFeaturesTransformerError
case class EmptyFieldNotAllowed(field: String)
    extends RasaFeaturesTransformerError
