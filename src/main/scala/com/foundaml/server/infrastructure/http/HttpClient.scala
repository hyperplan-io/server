package com.foundaml.server.infrastructure.http

import org.http4s.client.blaze.BlazeClientBuilder
import scalaz.zio.Task
import scalaz.zio.interop.catz._

import scala.concurrent.ExecutionContext

object HttpClient {

  def test(implicit ec: ExecutionContext) = {
    BlazeClientBuilder[Task](ec).resource.use { client =>
      // use `client` here and return an `IO`.
      // the client will be acquired and shut down
      // automatically each time the `IO` is run.
      Task.unit
    }
  }

}
