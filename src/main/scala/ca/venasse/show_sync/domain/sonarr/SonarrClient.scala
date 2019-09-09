package ca.venasse.show_sync.domain.sonarr

import ca.venasse.show_sync.domain.sonarr.domain.{QueueItem, SonarrServer}
import zio.{Ref, ZIO}

trait SonarrClient {

  val sonarr: SonarrClient.Service[Any]

}

object SonarrClient {

  trait Service[R] {

    def fetchQueue(server: SonarrServer): ZIO[R, Throwable, List[QueueItem]]

  }

  case class InMemorySonarrServer(queuedItems: Ref[List[QueueItem]]) extends Service[Any] {

    override def fetchQueue(server: SonarrServer): ZIO[Any, Throwable, List[QueueItem]] =
      queuedItems.get

  }

}