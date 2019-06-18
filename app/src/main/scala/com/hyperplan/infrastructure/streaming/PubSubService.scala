package com.hyperplan.infrastructure.streaming

import com.hyperplan.infrastructure.logging.IOLogging
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}
import cats.effect.IO
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.googlecloud.pubsub.PubSubConfig
import akka.stream.alpakka.googlecloud.pubsub.PubSubMessage
import java.{util => ju}
import akka.stream.alpakka.googlecloud.pubsub.PublishRequest
import akka.stream.scaladsl.Source
import akka.stream.alpakka.googlecloud.pubsub.scaladsl.GooglePubSub
import akka.stream.scaladsl.Sink

class PubSubService(pubSubConfig: PubSubConfig)(implicit system: ActorSystem, materializer: ActorMaterializer) extends IOLogging {

  def publish[Data](
      data: Data,
      topic: String
  )(implicit circeEncoder: io.circe.Encoder[Data]) = {
    val json = circeEncoder(data).noSpaces
    for {
      data <- IO(new String(ju.Base64.getEncoder.encode(circeEncoder(data).noSpaces.getBytes)))
      id <- IO(ju.UUID.randomUUID).map(_.toString)
      message <- IO(PubSubMessage(messageId = id, data = data))
      publishRequest <- IO(PublishRequest(scala.collection.immutable.Seq(message)))
      source <- IO(Source.single(publishRequest))
      publishFlow <- IO(GooglePubSub.publish(topic, pubSubConfig))
      messageIds <- IO(source.via(publishFlow).runWith(Sink.seq))
    } yield ()
  }
}

object PubSubService {
  def apply(projectId: String)(implicit system: ActorSystem, materializer: ActorMaterializer): IO[PubSubService] = {
    val privateKey = ""
    val clientEmail = ""
    val apiKey = ""
    IO(
      PubSubConfig(projectId, clientEmail, privateKey)
    ).map(config=> new PubSubService(config))
  }
}
