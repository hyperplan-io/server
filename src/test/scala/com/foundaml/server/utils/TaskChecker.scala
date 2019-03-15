package com.foundaml.server.utils

import doobie.scalatest.imports._
import doobie.imports._

import cats.effect.Effect

import org.scalatest._

import scalaz.zio.{IO, Task}
import scalaz.zio.interop.catz._

trait TaskChecker extends Checker[Task] {
  self: Assertions =>
  val M: Effect[Task] = implicitly
}
