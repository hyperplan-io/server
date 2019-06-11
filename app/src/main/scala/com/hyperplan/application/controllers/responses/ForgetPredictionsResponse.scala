package com.hyperplan.application.controllers.responses

case class ForgetPredictionsResponse(
    entityName: String,
    entityId: String,
    count: Int
)
