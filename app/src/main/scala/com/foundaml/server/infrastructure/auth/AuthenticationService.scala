package com.foundaml.server.infrastructure.auth

import java.time.Instant
import com.auth0.jwt.JWT
import cats.effect.IO
import cats.implicits._
import java.{util => ju}
import org.apache.commons.codec.binary.Base64
import com.auth0.jwt.algorithms.Algorithm
import com.foundaml.server.infrastructure.serialization.auth.AuthenticationScopeSerializer
import java.security.interfaces.{RSAPublicKey, RSAPrivateKey}
import java.security.spec.PKCS8EncodedKeySpec
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

trait AuthenticationService {
  def generateToken(
      data: AuthenticationService.AuthenticationData,
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ): IO[AuthenticationService.Token]
  def validate(
      token: AuthenticationService.Token,
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ): IO[AuthenticationService.AuthenticationData]
}

object AuthenticationService {

  sealed trait PublicKey
  case class JwtPublicKey(key: RSAPublicKey) extends PublicKey

  sealed trait PrivateKey
  case class JwtPrivateKey(key: RSAPrivateKey) extends PrivateKey

  case class Token(token: String)
  case class AuthenticationData(
      scope: List[AuthenticationScope],
      issuer: String,
      expiresAt: Instant
  )

  sealed trait AuthenticationScope {
    val scope: String
  }
  case object AdminScope extends AuthenticationScope {
    val scope = "admin"
  }
  case object PredictionScope extends AuthenticationScope {
    val scope = "prediction"
  }

  sealed trait AuthenticationServiceError extends Throwable {
    val message: String
    override def getMessage() = message
  }
  case object IncompatibleKey extends AuthenticationServiceError {
    val message =
      "the keys are incompatible, you need to use JwtPublicKey and JwtPrivateKey with JwtAuthenticationService"
  }

}

object JwtAuthenticationService extends AuthenticationService {

  def privateKey(rawString: String): IO[AuthenticationService.JwtPrivateKey] =
    rawString
      .replace("-----BEGIN PRIVATE KEY-----", "")
      .replace("-----END PRIVATE KEY-----", "")
      .pure[IO]
      .flatMap { privateKeyString =>
        IO {
          val baseEncoded = Base64.decodeBase64(privateKeyString)
          val keySpec = new PKCS8EncodedKeySpec(baseEncoded)
          KeyFactory
            .getInstance("RSA")
            .generatePrivate(keySpec)
            .asInstanceOf[RSAPrivateKey]
        }
      }
      .map { key =>
        AuthenticationService.JwtPrivateKey(key)
      }

  def publicKey(rawString: String): IO[AuthenticationService.JwtPublicKey] =
    rawString
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .pure[IO]
      .flatMap { privateKeyString =>
        IO {
          val baseEncoded = Base64.decodeBase64(privateKeyString)
          val keySpec = new X509EncodedKeySpec(baseEncoded)
          KeyFactory
            .getInstance("RSA")
            .generatePublic(keySpec)
            .asInstanceOf[RSAPublicKey]
        }
      }
      .map { key =>
        AuthenticationService.JwtPublicKey(key)
      }

  def algorithm(
      jwtPublicKey: AuthenticationService.JwtPublicKey,
      jwtPrivateKey: AuthenticationService.JwtPrivateKey
  ) =
    Algorithm.RSA256(jwtPublicKey.key, jwtPrivateKey.key)

  def generateToken(
      data: AuthenticationService.AuthenticationData,
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ): IO[AuthenticationService.Token] = (publicKey, privateKey) match {
    case (
        jwtPublicKey: AuthenticationService.JwtPublicKey,
        jwtPrivateKey: AuthenticationService.JwtPrivateKey
        ) =>
      IO {
        JWT
          .create()
          .withIssuer(data.issuer)
          .withClaim(
            "scope",
            AuthenticationScopeSerializer.encodeJsonListString(data.scope)
          )
          .withExpiresAt(ju.Date.from(data.expiresAt))
      }.flatMap(
          builder => IO(builder.sign(algorithm(jwtPublicKey, jwtPrivateKey)))
        )
        .map { token =>
          AuthenticationService.Token(token)
        }
    case _ =>
      IO.raiseError(AuthenticationService.IncompatibleKey)
  }

  def validate(
      token: AuthenticationService.Token,
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ): IO[AuthenticationService.AuthenticationData] =
    (publicKey, privateKey) match {
      case (
          jwtPublicKey: AuthenticationService.JwtPublicKey,
          jwtPrivateKey: AuthenticationService.JwtPrivateKey
          ) =>
        IO {
          JWT
            .require(algorithm(jwtPublicKey, jwtPrivateKey))
            .build()
            .verify(token.token)
        }.flatMap { decoded =>
            IO {
              val issuer = decoded.getIssuer()
              val expiresAt = decoded.getExpiresAt().toInstant()
              val scope = decoded.getClaim("scope").asString()
              (scope, issuer, expiresAt)
            }
          }
          .flatMap {
            case (scopeJson, issuer, expiresAt) =>
              AuthenticationScopeSerializer
                .decodeJsonList(scopeJson)
                .fold[IO[AuthenticationService.AuthenticationData]](
                  err => IO.raiseError(new Exception("")),
                  scope =>
                    AuthenticationService
                      .AuthenticationData(scope, issuer, expiresAt)
                      .pure[IO]
                )
          }
      case _ =>
        IO.raiseError(AuthenticationService.IncompatibleKey)
    }
}
