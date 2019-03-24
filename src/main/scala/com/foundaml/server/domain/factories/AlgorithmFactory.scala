package com.foundaml.server.domain.factories

import com.foundaml.server.domain.models._
import com.foundaml.server.domain.models.errors.{
  AlgorithmDataIncorrect,
  AlgorithmError,
  ProjectDataInconsistent
}
import com.foundaml.server.domain.repositories.{
  AlgorithmsRepository,
  ProjectsRepository
}
import com.foundaml.server.infrastructure.logging.IOLogging
import scalaz.zio.{Task, ZIO}

class AlgorithmFactory(
    algorithmsRepository: AlgorithmsRepository
) extends IOLogging {
  def get(algorithmId: String): ZIO[Any, Throwable, Algorithm] = {
    algorithmsRepository.read(algorithmId).flatMap {
      case (
          id,
          Right(backend),
          projectId
          ) =>
        Task.succeed(
          Algorithm(id, backend, projectId)
        )

      case err =>
        println(err)
        Task.fail(AlgorithmDataIncorrect(algorithmId))
    }
  }

  def getForProject(projectId: String): ZIO[Any, Throwable, List[Algorithm]] = {
    algorithmsRepository.readForProject(projectId).flatMap { algorithmDatas =>
      val empty = (List.empty[AlgorithmError], List.empty[Algorithm])
      algorithmDatas.foldLeft(empty) {
        case (acc, elem) =>
          val (errors, algorithms) = acc
          elem match {
            case (
                id,
                Right(backend),
                _
                ) =>
              (errors, algorithms :+ Algorithm(id, backend, projectId))
            case err =>
              println(err)
              (errors :+ AlgorithmDataIncorrect(elem._1), algorithms)
          }
      } match {
        case (Nil, algorithms) =>
          Task.succeed(algorithms)
        case (errors, algorithms) =>
          warnLog(
            s"Some errors occurred while loading algorithms: ${errors.map(_.getMessage).mkString(", ")}"
          ) *> Task.succeed(algorithms)
      }
    }
  }
}
