package com.hyperplan.server.test

import io.circe.Json
import org.scalatest.Assertion
import io.circe.parser._
import org.scalatest.Assertions._

trait SerializerTester {

  def testEncoder[Data](data: Data)(
      testFunction: Json => Assertion
  )(implicit encoder: io.circe.Encoder[Data]) = {
    testFunction(
      encoder.apply(data)
    )
  }

  def testDecoder[Data](data: String)(
      testFunction: Data => Assertion
  )(implicit decoder: io.circe.Decoder[Data]): Assertion = {
    parse(data).fold(
      err => fail(err),
      json =>
        decoder
          .decodeJson(json)
          .fold(
            err => fail(err),
            data => testFunction(data)
          )
    )
  }
}
