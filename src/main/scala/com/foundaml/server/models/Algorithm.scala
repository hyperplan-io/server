package com.foundaml.server.models

import backends._
import java.util.UUID

case class Algorithm(
    id: String,
    backend: Backend
)
