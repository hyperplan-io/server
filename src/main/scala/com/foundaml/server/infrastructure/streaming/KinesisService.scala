package com.foundaml.server.infrastructure.streaming

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
