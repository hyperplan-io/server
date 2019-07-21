package com.hyperplan.test.application

import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import scala.util.Try

object ControllerTestUtils {
  def check[A](
      actual: IO[Response[IO]],
      expectedStatus: Status,
      expectedBody: Option[A]
  )(
      implicit ev: EntityDecoder[IO, A]
  ): Boolean = {
    val actualResp = actual.unsafeRunSync
    lazy val statusCheck = actualResp.status == expectedStatus
    lazy val bodyCheck = expectedBody.fold[Boolean](
      actualResp.body.compile.toVector.unsafeRunSync.isEmpty
    )( // Verify Response's body is empty.
      expected => actualResp.as[A].unsafeRunSync == expected
    )
    if (!statusCheck) {
      println(s"Expected status $expectedStatus but got $actualResp.status")
    }
    if (!bodyCheck) {
      println(
        s"Body is not as expected: ${actualResp.as[A].unsafeRunSync().toString}"
      )
    }
    statusCheck && bodyCheck
  }
}
