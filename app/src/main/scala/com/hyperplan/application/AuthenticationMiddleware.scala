package com.hyperplan.application

object AuthenticationMiddleware {

  import cats.effect.IO
  import cats.implicits._
  import org.http4s.HttpRoutes
  import org.http4s.Header
  import org.http4s.Request
  import cats.data.Kleisli
  import org.http4s.util.CaseInsensitiveString
  import org.http4s.headers.Authorization
  import com.hyperplan.infrastructure.auth.AuthenticationService
  import scala.language.higherKinds
  import cats.Monad
  import org.http4s.Credentials
  import org.http4s.AuthScheme
  import org.http4s.Response
  import org.http4s.Status
  import cats.data.OptionT
  import com.hyperplan.infrastructure.auth.JwtAuthenticationService

  def jwtAuthenticateWithCertificate(
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  )(
      service: HttpRoutes[IO],
      scope: AuthenticationService.AuthenticationScope
  ): HttpRoutes[IO] = Kleisli { req: Request[IO] =>
    val validateRequest = getCredentials[IO] andThen getEncodedJwtToken
    validateRequest(req).fold(
      Response[IO](status = Status.Unauthorized).pure[OptionT[IO, ?]]
    ) { jwtToken =>
      validateWithCertificate(jwtToken, scope, publicKey, privateKey)
        .flatMap { authenticationData =>
          service(req)
        }
        .handleErrorWith {
          case err =>
            Response[IO](status = Status.Unauthorized).pure[OptionT[IO, ?]]
        }
    }
  }

  def jwtAuthenticateWithSecret(secret: String)(
      service: HttpRoutes[IO],
      scope: AuthenticationService.AuthenticationScope
  ): HttpRoutes[IO] = Kleisli { req: Request[IO] =>
    val validateRequest = getCredentials[IO] andThen getEncodedJwtToken
    validateRequest(req).fold(
      Response[IO](status = Status.Unauthorized).pure[OptionT[IO, ?]]
    ) { jwtToken =>
      validateWithSecret(jwtToken, scope, secret)
        .flatMap { authenticationData =>
          service(req)
        }
        .handleErrorWith {
          case err =>
            Response[IO](status = Status.Unauthorized).pure[OptionT[IO, ?]]
        }
    }
  }

  def getCredentials[F[_]]: Kleisli[Option, Request[F], Credentials] = Kleisli {
    req =>
      req.headers.get(Authorization).map(_.credentials)
  }

  def getEncodedJwtToken: Kleisli[Option, Credentials, String] = Kleisli {
    case Credentials.Token(AuthScheme.Bearer, token) => token.some
    case _ => none[String]
  }

  def validateWithCertificate(
      jwtToken: String,
      scope: AuthenticationService.AuthenticationScope,
      publicKey: AuthenticationService.PublicKey,
      privateKey: AuthenticationService.PrivateKey
  ): OptionT[IO, AuthenticationService.AuthenticationData] =
    OptionT.liftF(
      JwtAuthenticationService.validate(jwtToken, scope, publicKey, privateKey)
    )

  def validateWithSecret(
      jwtToken: String,
      scope: AuthenticationService.AuthenticationScope,
      secret: String
  ): OptionT[IO, AuthenticationService.AuthenticationData] =
    OptionT.liftF(
      JwtAuthenticationService.validate(jwtToken, scope, secret)
    )

}
