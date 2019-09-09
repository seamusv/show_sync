package ca.venasse.show_sync

import ca.venasse.show_sync.config.{Config, LocalSettings, RSyncSettings}
import ca.venasse.show_sync.domain.fetcher.FetchClient
import ca.venasse.show_sync.domain.sonarr.SonarrClient
import ca.venasse.show_sync.infrastructure.clients.{Http4sSonarrClient, RsyncFetchClient}
import org.http4s.client.Client
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.{RIO, Task}

object ApplicationEnvironment {
  type AppEnvironment = FetchClient with SonarrClient with Blocking with Console with Clock

  type AppTask[A] = RIO[AppEnvironment, A]

  def appEnv(config: Config, httpClient: Client[Task])(base: Clock with Console with Blocking): AppEnvironment =
    new FetchClient with SonarrClient with Clock with Console with Blocking {
      private val fetchClient = new RsyncFetchClient {
        override protected val rsyncSettings: RSyncSettings = config.rsync
        override protected val localSettings: LocalSettings = config.local
        override val blocking: Blocking.Service[Any] = base.blocking
      }

      private val http4sSonarrClient = new Http4sSonarrClient {
        override protected val client: Client[Task] = httpClient
        override val blocking: Blocking.Service[Any] = base.blocking
      }

      override val fetcher: FetchClient.Service[Any] = fetchClient.fetcher
      override val sonarr: SonarrClient.Service[Any] = http4sSonarrClient.sonarr

      override val clock: Clock.Service[Any] = base.clock
      override val console: Console.Service[Any] = base.console
      override val blocking: Blocking.Service[Any] = base.blocking
    }


}
