package ca.venasse.show_sync.domain

import ca.venasse.show_sync.domain.sonarr.domain.SonarrServer
import zio.ZIO

package object sonarr extends SonarrClient.Service[SonarrClient] {

  override def fetchQueue(server: SonarrServer): ZIO[SonarrClient, Throwable, List[domain.QueueItem]] =
    ZIO.accessM(_.sonarr.fetchQueue(server))

}
