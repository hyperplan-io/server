/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.application

case class PredictionConfig(
    storeInPostgresql: Boolean
)

case class KinesisConfig(
    enabled: Boolean,
    predictionsStream: String,
    examplesStream: String,
    region: String
)
case class GCPConfig(
    projectId: String,
    privateKey: String,
    clientEmail: String,
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
