package ca.venasse.show_sync.infrastructure.clients

import ca.venasse.show_sync.domain.sonarr.SonarrClient
import ca.venasse.show_sync.domain.sonarr.domain.{QueueItem, SonarrServer}
import io.circe._
import io.circe.generic.extras.defaults._
import io.circe.generic.extras.semiauto._
import org.http4s.client.Client
import zio.blocking.Blocking
import zio.{Task, ZIO}

trait Http4sSonarrClient extends SonarrClient with Blocking with JsonSupportEndpoint[Any] {

  protected val client: Client[Task]

  override val sonarr: SonarrClient.Service[Any] = new SonarrClient.Service[Any] {

    import Http4sSonarrClient._

    override def fetchQueue(server: SonarrServer): ZIO[Any, Throwable, List[QueueItem]] =
      blocking.blocking {
        client.expect[List[QueueItem]](s"${server.baseUrl}/api/queue?apikey=${server.apiKey}")
      }
  }
}

object Http4sSonarrClient {
  implicit val decodeQueueItem: Decoder[QueueItem] = deriveConfiguredDecoder[QueueItem]

}