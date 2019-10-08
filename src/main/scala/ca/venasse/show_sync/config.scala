package ca.venasse.show_sync

import ca.venasse.show_sync.domain.sonarr.domain.SonarrServer
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio._

import scala.concurrent.ExecutionContext

object config {

  case class Config(
                     servers: List[SonarrServer],
                     local: LocalSettings,
                     rsync: RSyncSettings,
                   )

  case class RSyncSettings(
                            sourcePrefix: String,
                            passwordFile: String,
                          )

  case class LocalSettings(
                            stagePath: String,
                            completedPath: String,
                          )

  import zio.interop.catz._
  def makeHttp4sClient[R](blockingEC: ExecutionContext)(implicit rts: Runtime[R]): ZManaged[Any, Throwable, Client[Task]] = {
    val res = BlazeClientBuilder[Task](blockingEC)
      .allocated
      .map { case (client, cleanUp) =>
        Reservation(ZIO.succeed(client), _ => cleanUp.orDie)
      }
      .uninterruptible

    ZManaged(res)
  }
}