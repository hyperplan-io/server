package com.foundaml.server.infrastructure.streaming

import com.foundaml.server.infrastructure.logging.IOLogging
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}
import cats.effect.IO

class PubSubService(publisher: Publisher) extends IOLogging {

  def publish[Data](
      data: Data
  )(implicit circeEncoder: io.circe.Encoder[Data]) = {
    val json = circeEncoder(data).noSpaces
    for {
      dataBytes <- IO(ByteString.copyFromUtf8(json))
      message <- IO(PubsubMessage.newBuilder().setData(dataBytes).build())
      _ <- logger.info(s"message id : ${message.getMessageId}")
      apiFuture <- IO(publisher.publish(message))
      _ <- logger.info(apiFuture.get())
    } yield ()
  }
}

object PubSubService {
  def apply(projectId: String, topicId: String): IO[PubSubService] = {
    IO(
      ProjectTopicName.of(projectId, topicId)
    ).flatMap { topic =>
        IO(Publisher.newBuilder(topic).build())
      }
      .map(publisher => new PubSubService(publisher))
  }
}
