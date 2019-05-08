package com.foundaml.server.infrastructure.streaming

import com.foundaml.server.infrastructure.logging.IOLogging
import cats.effect.IO
import io.circe.Encoder

import fs2.kafka._

class KafkaService(topic: String, stream: fs2.Stream[IO, KafkaProducer[IO,String,String]]) extends IOLogging {

  def publish[Data](
    data: Data,
    partitionKey: String
  )(implicit circeEncoder: Encoder[Data]) = {
    val json = circeEncoder(data).noSpaces
    val record = ProducerRecord(topic, partitionKey, json)
    ProducerMessage.one(record)
  }

}

object KafkaService {
  import org.apache.kafka.common.serialization.StringSerializer
  import cats.effect.ContextShift
  def apply(topic: String, bootstrapServers: String)(implicit cs: ContextShift[IO]): KafkaService = {
    val producerSettings = ProducerSettings[String, String].withBootstrapServers(bootstrapServers)
    val producer = fs2.kafka.producerStream[IO].using(producerSettings)
    new KafkaService(topic, producer)
  }
}
