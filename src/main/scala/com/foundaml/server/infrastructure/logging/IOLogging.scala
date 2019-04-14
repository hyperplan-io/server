package com.foundaml.server.infrastructure.logging

import cats.effect.IO

trait IOLogging {

  type ZIOLog = IO[Unit]

  trait Logger {
    def trace[A](a: A): ZIOLog
    def debug[A](a: A): ZIOLog
    def info[A](a: A): ZIOLog
    def warn[A](a: A): ZIOLog
    def error[A](a: A): ZIOLog
  }

  implicit lazy val logger: Logger = new Logger {
    private def log[A](level: String)(a: A): IO[Unit] =
      IO(println(s"[$level] $a"))

    def trace[A](a: A): ZIOLog = log("TRACE")(a)
    def debug[A](a: A): ZIOLog = log("DEBUG")(a)
    def info[A](a: A): ZIOLog = log("INFO")(a)
    def warn[A](a: A): ZIOLog = log("WARN")(a)
    def error[A](a: A): ZIOLog = log("ERROR")(a)
  }
}
