package com.foundaml.server.domain

case class KinesisConfig(
    enabled: Boolean,
    predictionsStream: String,
    examplesStream: String
)

case class PostgreSqlConfig(
    host: String,
    port: Int,
    database: String,
    username: String,
    password: String
)
case class DatabaseConfig(postgresql: PostgreSqlConfig)

case class FoundaMLConfig(kinesis: KinesisConfig, database: DatabaseConfig)
