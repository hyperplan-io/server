package com.foundaml.server.infrastructure.logging

import scalaz.zio.console.Console
import scalaz.zio.{Task, ZIO, console}

trait IOLogging {

  type ZIOLog = Task[Unit]

  trait Logger {
    def trace[A](a: A): ZIOLog
    def debug[A](a: A): ZIOLog
    def info[A](a: A): ZIOLog
    def warn[A](a: A): ZIOLog
    def error[A](a: A): ZIOLog
  }

  implicit lazy val logger: Logger = new Logger {
    private def log[A](level: String)(a: A): ZIOLog =
      Task(println(s"[$level] $a"))

    def trace[A](a: A): ZIOLog = log("TRACE")(a)
    def debug[A](a: A): ZIOLog = log("DEBUG")(a)
    def info[A](a: A): ZIOLog = log("INFO")(a)
    def warn[A](a: A): ZIOLog = log("WARN")(a)
    def error[A](a: A): ZIOLog = log("ERROR")(a)
  }
}
