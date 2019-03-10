package com.foundaml.server.services.infrastructure.streaming

import com.foundaml.server.services.infrastructure.serialization._

import io.circe.Json
import scalaz.zio.{IO, Task}

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.amazonaws.auth.profile.ProfileCredentialsProvider

import scala.util.Try

import java.nio.ByteBuffer

case class KinesisPutError(message: String) extends Throwable

class KinesisService(kinesisClient: AmazonKinesis, jsonService: JsonService) {

  import io.circe._, io.circe.generic.auto._, io.circe.parser._,
  io.circe.syntax._

  def put[Data](
      data: Data,
      streamName: String,
      partitionKey: String
  )(implicit circeEncoder: io.circe.Encoder[Data]): Task[Unit] = {
    val dataJson = jsonService.toJson(data)
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
      region: String,
      jsonService: JsonService
  ): Task[KinesisService] =
    for {
      credentialsProvider <- getProfileCredentialsProvider()
      kinesisClient <- buildKinesisClient(credentialsProvider, region)
      kinesisService <- IO(new KinesisService(kinesisClient, jsonService))
    } yield kinesisService

  def buildKinesisClient(
      credentialsProvider: AWSCredentialsProvider,
      region: String
  ) =
    for {
      builder <- IO(AmazonKinesisClientBuilder.standard())
      builderWithCredentials <- IO(builder.withCredentials(credentialsProvider))
      builderWithRegion <- IO(builderWithCredentials.withRegion(region))
      amazonKinesis <- IO(builderWithRegion.build())
    } yield amazonKinesis

  def getProfileCredentialsProvider() = IO(new ProfileCredentialsProvider())

}
