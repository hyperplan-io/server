package com.hyperplan.infrastructure.streaming

import cats.effect.IO
import cats.implicits._
import io.circe.Encoder

import fs2.kafka._
import cats.effect.Timer
import cats.effect.ContextShift
import cats.effect.concurrent.Ref

import com.hyperplan.infrastructure.logging.IOLogging
import com.hyperplan.domain.errors._

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class KafkaService(
    defaultTopic: String,
    stream: fs2.Stream[IO, KafkaProducer[IO, String, String]],
    health: Ref[IO, Boolean]
) extends IOLogging {
  val producerTimeout = Duration(3, TimeUnit.SECONDS)
  def publish[Data](
      data: Data,
      partitionKey: String,
      overrideTopic: Option[String]
  )(
      implicit circeEncoder: Encoder[Data],
      cs: ContextShift[IO],
      timer: Timer[IO]
  ) = {
    val json = circeEncoder(data).noSpaces
    val produceIO = stream
      .evalMap { producer =>
        val record = ProducerRecord(
          overrideTopic.getOrElse(defaultTopic),
          partitionKey,
          json
        )
        val message = ProducerMessage.one(record)
        producer.produce(message)
      }
      .compile
      .drain
    IO.race(
        timer.sleep(producerTimeout),
        produceIO
      )
      .flatMap {
        case Right(_) => logger.debug("published message in kafka")
        case Left(_) =>
          health
            .modify(h => false -> h)
            .flatMap(
              _ =>
                logger.warn("failed to publish in kafka, timeout") *> IO
                  .raiseError(DataStreamTimedOut("Kafka producer timed out"))
            )
      }
  }

  def isHealthy = health.get

}

object KafkaService {
  import org.apache.kafka.common.serialization.StringSerializer
  import cats.effect.ContextShift
  def apply(defaultTopic: String, bootstrapServers: String)(
      implicit cs: ContextShift[IO]
  ): IO[KafkaService] = {
    val producerSettings =
      ProducerSettings[String, String].withBootstrapServers(bootstrapServers)
    val producer = fs2.kafka.producerStream[IO].using(producerSettings)
    Ref.of[IO, Boolean](true).flatMap { ref =>
      IO.pure(new KafkaService(defaultTopic, producer, ref))
    }
  }
}
