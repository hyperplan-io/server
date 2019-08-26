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

package com.hyperplan.infrastructure.streaming

import cats.effect.IO
import com.amazonaws.auth.{
  AWSCredentialsProvider,
  DefaultAWSCredentialsProviderChain
}
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.model.PutRecordRequest
import java.nio.ByteBuffer

case class KinesisPutError(message: String) extends Throwable

class KinesisService(kinesisClient: AmazonKinesis) {

  def put[Data](
      data: Data,
      streamName: String,
      partitionKey: String
  )(implicit circeEncoder: io.circe.Encoder[Data]): IO[Unit] = {
    val dataJson = circeEncoder(data).noSpaces
    for {
      jsonBytes <- IO(dataJson.getBytes)
      request <- IO(new PutRecordRequest())
      _ <- IO(request.setStreamName(streamName))
      _ <- IO(request.setPartitionKey(partitionKey))
      buffer <- IO(ByteBuffer.wrap(jsonBytes))
      _ <- IO(request.setData(buffer))
      _ <- IO(kinesisClient.putRecord(request))
    } yield ()
  }
}

object KinesisService {

  def apply(
      region: String
  ): IO[KinesisService] =
    for {
      credentialsProvider <- getCredentialsProvider
      kinesisClient <- buildKinesisClient(credentialsProvider, region)
      kinesisService <- IO(new KinesisService(kinesisClient))
    } yield kinesisService

  def buildKinesisClient(
      credentialsProvider: AWSCredentialsProvider,
      region: String
  ): IO[AmazonKinesis] =
    for {
      builder <- IO(AmazonKinesisClientBuilder.standard())
      builderWithCredentials <- IO(builder.withCredentials(credentialsProvider))
      builderWithRegion <- IO(builderWithCredentials.withRegion(region))
      amazonKinesis <- IO(builderWithRegion.build())
    } yield amazonKinesis

  def getCredentialsProvider =
    IO(new DefaultAWSCredentialsProviderChain())

}
