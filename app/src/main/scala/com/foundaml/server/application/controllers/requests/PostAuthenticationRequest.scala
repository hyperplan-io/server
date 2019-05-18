package com.foundaml.server.controllers.requests

case class PostAuthenticationRequest(
    username: String,
    password: String
)
