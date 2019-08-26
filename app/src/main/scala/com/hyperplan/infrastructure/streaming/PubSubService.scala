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
