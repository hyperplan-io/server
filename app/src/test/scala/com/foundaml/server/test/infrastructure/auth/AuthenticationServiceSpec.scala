package com.foundaml.server.test.infrastructure.auth

import org.scalatest.{FlatSpec, Matchers}
import com.foundaml.server.infrastructure.auth._
import scala.io.Source
import java.time.Instant
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit
import java.time.temporal.ChronoUnit
import cats.implicits._

class AuthenticationServiceSpec extends FlatSpec with Matchers {

  def loadResource(name: String): String = {
    val iter = Source.fromResource(name).getLines()
    var content = ""
    while (iter.hasNext) {
      content = s"$content ${iter.next()}"
    }
    content
  }

  it should "fail to generate an invalid private key" in {
    val privateKey = JwtAuthenticationService.privateKey("toto")
    assertThrows[java.security.spec.InvalidKeySpecException](
      privateKey.unsafeRunSync()
    )
  }

  it should "fail to generate an invalid public key" in {
    val publicKey = JwtAuthenticationService.publicKey("toto")
    assertThrows[java.security.spec.InvalidKeySpecException](
      publicKey.unsafeRunSync()
    )
  }

  it should "correctly generate a valid public key" in {
    val publicKeyStr = loadResource("public.der")
    val publicKey = JwtAuthenticationService.publicKey(publicKeyStr)
    publicKey.unsafeRunSync()
  }

  it should "correctly generate a valid private key" in {
    val privateKeyStr = loadResource("private.pem")
    val privateKey = JwtAuthenticationService.privateKey(privateKeyStr)
    privateKey.unsafeRunSync()
  }

  it should "correctly encode and decode a token jwt token with an expiration date" in {
    val publicKeyStr = loadResource("public.der")
    val publicKey =
      JwtAuthenticationService.publicKey(publicKeyStr).unsafeRunSync
    val privateKeyStr = loadResource("private.pem")
    val privateKey =
      JwtAuthenticationService.privateKey(privateKeyStr).unsafeRunSync

    val expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
    val authData = AuthenticationService.AuthenticationData(
      List(
        AuthenticationService.AdminScope
      ),
      "test",
      expiresAt.some
    )
    val token = JwtAuthenticationService
      .generateToken(
        authData,
        publicKey,
        privateKey
      )
      .unsafeRunSync

    val decoded = JwtAuthenticationService
      .validate(token, AuthenticationService.AdminScope, publicKey, privateKey)
      .unsafeRunSync
    decoded.scope should be(authData.scope)
    decoded.issuer should be(authData.issuer)
    println(
      (expiresAt.toEpochMilli()) - decoded.expiresAt.get.toEpochMilli
    )
    // Instant to Date conversation loses some information
    // We check if the dates are close enough
    // (less than 1 second of difference)
    assert(
      expiresAt.toEpochMilli() - decoded.expiresAt.get.toEpochMilli < 1000
    )
  }
  it should "correctly encode and decode a token jwt token without an expiration date" in {
    val publicKeyStr = loadResource("public.der")
    val publicKey =
      JwtAuthenticationService.publicKey(publicKeyStr).unsafeRunSync
    val privateKeyStr = loadResource("private.pem")
    val privateKey =
      JwtAuthenticationService.privateKey(privateKeyStr).unsafeRunSync

    val authData = AuthenticationService.AuthenticationData(
      List(
        AuthenticationService.AdminScope
      ),
      "test",
      None
    )
    val token = JwtAuthenticationService
      .generateToken(
        authData,
        publicKey,
        privateKey
      )
      .unsafeRunSync

    val decoded = JwtAuthenticationService
      .validate(token, AuthenticationService.AdminScope, publicKey, privateKey)
      .unsafeRunSync
    decoded.scope should be(authData.scope)
    decoded.issuer should be(authData.issuer)
    assert(
      decoded.expiresAt.isEmpty
    )
  }
}
