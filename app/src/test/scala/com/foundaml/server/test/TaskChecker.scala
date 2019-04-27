package com.foundaml.server.test

import cats.effect.Effect
import doobie.scalatest.imports.Checker
import org.scalatest.Assertions
import cats.effect.IO
import cats.implicits._

trait TaskChecker extends Checker[IO] {
  self: Assertions =>
  val M: Effect[IO] = implicitly
}
