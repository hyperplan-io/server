package com.hyperplan.application

case class PredictionConfig(
    storeInPostgresql: Boolean
)

case class KinesisConfig(
    enabled: Boolean,
    predictionsStream: String,
    examplesStream: String
)
case class GCPConfig(
    projectId: String,
    pubsub: PubSubConfig
)
case class KafkaConfig(
    enabled: Boolean,
    topic: String,
    bootstrapServers: String
)
case class PubSubConfig(enabled: Boolean, predictionsTopicId: String)
case class PostgreSqlConfig(
    host: String,
    port: Int,
    database: String,
    username: String,
    password: String,
    schema: String,
    threadPool: Int
)
case class DatabaseConfig(postgresql: PostgreSqlConfig)
case class EncryptionConfig(
    publicKey: Option[String],
    privateKey: Option[String],
    secret: Option[String],
    issuer: String
)
case class AdminCredentials(
    username: String,
    password: String
)
case class SecurityConfig(
    protectPredictionRoute: Boolean
)
case class ApplicationConfig(
    kinesis: KinesisConfig,
    gcp: GCPConfig,
    kafka: KafkaConfig,
    database: DatabaseConfig,
    encryption: EncryptionConfig,
    credentials: AdminCredentials,
    prediction: PredictionConfig,
    security: SecurityConfig
)
