package com.foundaml.server.services.infrastructure.storage

import doobie._
import doobie.implicits._

import scalaz.zio.Task

class PostgresqlService {

  def connect(host: String, port: String, database: String, username: String, password: String) = {
    val xa = Transactor.fromDriverManager[Task](
      "org.postgresql.Driver", 
      s"jdbc:postgresql://$host:$port/$database", 
      username, 
      password 
    )
  }

}
