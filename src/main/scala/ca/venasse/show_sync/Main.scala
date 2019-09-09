package ca.venasse.show_sync

import ca.venasse.show_sync.config.{Config, _}
import ca.venasse.show_sync.domain.Monitor
import pureconfig.generic.auto._
import zio._
import zio.blocking.Blocking
import zio.console._

object Main extends App {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      cfg <- ZIO.fromEither(pureconfig.loadConfig[Config])

      blockingEC <- ZIO.environment[Blocking].flatMap(_.blocking.blockingExecutor).map(_.asEC)
      httpClientR <- ZIO.runtime[Environment].map { implicit rts => makeHttp4sClient(blockingEC) }

      _ <- putStrLn(s"SERVERS: ${cfg.servers}")

      program <- httpClientR.use { client =>
        Monitor.monitorQueue(cfg.servers)
          .provideSome[Environment](ApplicationEnvironment.appEnv(cfg, client))
      }
    } yield program)
      .foldM(
        err => putStrLn(s"Error: $err") *> ZIO.succeed(1),
        r => putStrLn(s"OK... $r") *> ZIO.succeed(0)
      )
}
