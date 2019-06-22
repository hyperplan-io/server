package com.hyperplan.infrastructure.streaming

import com.hyperplan.infrastructure.logging.IOLogging
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

class PubSubService(pubSubConfig: PubSubConfig)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer
) extends IOLogging {

  def publish[Data](
      data: Data,
      topic: String
  )(implicit circeEncoder: io.circe.Encoder[Data]): IO[Seq[String]] = {
    val json = circeEncoder(data).noSpaces
    for {
      data <- IO(
        new String(
          ju.Base64.getEncoder.encode(circeEncoder(data).noSpaces.getBytes)
        )
      )
      id <- IO(ju.UUID.randomUUID).map(_.toString)
      message <- IO(PubSubMessage(messageId = id, data = data))
      publishRequest <- IO(
        PublishRequest(scala.collection.immutable.Seq(message))
      )
      source <- IO(Source.single(publishRequest))
      publishFlow <- IO(GooglePubSub.publish(topic, pubSubConfig))
      messageIds <- IO.fromFuture(
        IO((source.via(publishFlow).runWith(Sink.seq)))
      )
    } yield messageIds.flatten
  }
}

object PubSubService {
  def apply(projectId: String, privateKey: String, clientEmail: String)(
      implicit system: ActorSystem,
      materializer: ActorMaterializer
  ): IO[PubSubService] = {
    IO(
      PubSubConfig(projectId, clientEmail, privateKey)
    ).map(config => new PubSubService(config))
  }
}
