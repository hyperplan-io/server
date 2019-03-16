package com.foundaml.server.test

import cats.effect.Effect
import doobie.scalatest.imports.Checker
import org.scalatest.Assertions
import scalaz.zio.Task
import scalaz.zio.interop.catz._

trait TaskChecker extends Checker[Task] {
  self: Assertions =>
  val M: Effect[Task] = implicitly
}
