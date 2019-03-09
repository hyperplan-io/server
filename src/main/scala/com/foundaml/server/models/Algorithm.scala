package com.foundaml.server.models

import java.util.UUID

case class Algorithm[FeatureType, LabelType](
  id: String,
  backend: Backend
)
