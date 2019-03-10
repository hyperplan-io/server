package com.foundaml.server.models

import backends._
import java.util.UUID

case class Algorithm[FeatureType, LabelType](
  id: String,
  backend: Backend
)
