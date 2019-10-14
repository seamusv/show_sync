package ca.venasse.show_sync

import java.util.concurrent.TimeUnit

import ca.venasse.show_sync.clients.{ArchiveClient, FilesystemClient, Http4sClient, RsyncClient}
import ca.venasse.show_sync.config.{Config, _}
import ca.venasse.show_sync.domain.{MediaClient, Monitor, SyncClient}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console._
import zio.duration.Duration

import scala.concurrent.ExecutionContext

object Main extends App {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (for {
      cfg <- ZIO.fromEither(ConfigSource.default.load[Config])

      _blockingEC <- ZIO.environment[Blocking].flatMap(_.blocking.blockingExecutor).map(_.asEC)
      _runtime <- ZIO.runtime[Environment]

      _ <- putStrLn(s"SERVERS: ${cfg.servers}")

      monitor = Monitor.processQueues(cfg.servers).repeat(ZSchedule.spaced(Duration(5, TimeUnit.MINUTES)))

      program <- monitor
        .provide {
          new ArchiveClient.Live with FilesystemClient.Live with Http4sClient.Live with RsyncClient.Live with MediaClient.Live with SyncClient.Live with Blocking.Live with Clock.Live {
            override implicit val runtime: Runtime[Any] = _runtime
            override val blockingEC: ExecutionContext = _blockingEC
            override val rsyncSettings: RSyncSettings = cfg.rsync
            override val localSettings: LocalSettings = cfg.local
          }
        }
    } yield program)
      .foldM(
        err => putStrLn(s"Error: $err") *> ZIO.succeed(1),
        r => putStrLn(s"OK... $r") *> ZIO.succeed(0)
      )
}
