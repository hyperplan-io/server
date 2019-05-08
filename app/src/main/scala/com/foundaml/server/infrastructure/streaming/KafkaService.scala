package com.foundaml.server.infrastructure.streaming

import com.foundaml.server.infrastructure.logging.IOLogging
import cats.effect.IO
import io.circe.Encoder

import fs2.kafka._
import cats.effect.Timer
import cats.effect.ContextShift
import cats.effect.concurrent.Ref

import com.foundaml.server.domain.models.errors._

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class KafkaService(
    topic: String,
    stream: fs2.Stream[IO, KafkaProducer[IO, String, String]],
    health: Ref[IO, Boolean]
) extends IOLogging {

  def publish[Data](
      data: Data,
      partitionKey: String
  )(
      implicit circeEncoder: Encoder[Data],
      cs: ContextShift[IO],
      timer: Timer[IO]
  ) = {
    val json = circeEncoder(data).noSpaces
    val produceIO = stream
      .evalMap { producer =>
        val record = ProducerRecord(topic, partitionKey, json)
        val message = ProducerMessage.one(record)
        producer.produce(message)
      }
      .compile
      .drain
    IO.race(
        produceIO,
        timer.sleep(Duration(3, TimeUnit.SECONDS))
      )
      .flatMap {
        case Left(_) => IO.unit
        case Right(_) =>
          health
            .modify(h => false -> h)
            .flatMap(
              _ => IO.raiseError(DataStreamTimedOut("Kafka producer timed out"))
            )
      }
  }

  def isHealthy = health.get

}

object KafkaService {
  import org.apache.kafka.common.serialization.StringSerializer
  import cats.effect.ContextShift
  def apply(topic: String, bootstrapServers: String)(
      implicit cs: ContextShift[IO]
  ): IO[KafkaService] = {
    val producerSettings =
      ProducerSettings[String, String].withBootstrapServers(bootstrapServers)
    val producer = fs2.kafka.producerStream[IO].using(producerSettings)
    Ref.of[IO, Boolean](true).flatMap { ref =>
      IO.pure(new KafkaService(topic, producer, ref))
    }
  }
}
