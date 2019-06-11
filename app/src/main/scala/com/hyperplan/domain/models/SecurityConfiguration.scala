package com.hyperplan.domain.models

sealed trait Encryption
case object PlainEncryption extends Encryption
case object AESEncryption extends Encryption

sealed trait SecurityConfiguration {
  val encryption: Encryption
  val headers: List[(String, String)]
}

case class PlainSecurityConfiguration(
    headers: List[(String, String)]
) extends SecurityConfiguration {
  val encryption = PlainEncryption
}

case class EncryptedSecurityConfiguration(
    headers: List[(String, String)]
) extends SecurityConfiguration {
  val encryption = AESEncryption

  def decrypt(key: String): PlainSecurityConfiguration = ???

}
