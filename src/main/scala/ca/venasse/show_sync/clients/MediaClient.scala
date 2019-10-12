package ca.venasse.show_sync.clients

import ca.venasse.show_sync.domain.{InvalidApiKeyAppError, QueueItem, SonarrServer}
import io.circe.generic.auto._
import org.http4s.circe.jsonOf
import org.http4s.{EntityDecoder, Header, Headers, Method, Request, Status, Uri}
import zio.interop.catz._
import zio.{Task, ZIO}

trait MediaClient {

  val mediaClient: MediaClient.Service[Any]

}

object MediaClient {

  trait Service[R] {

    def fetchQueue(server: SonarrServer): ZIO[R, Throwable, List[QueueItem]]

  }

  trait Live extends MediaClient {

    val http4sClient: Http4sClient.Service[Any]

    override val mediaClient: Service[Any] = new Service[Any] {

      implicit val decoder: EntityDecoder[Task, List[QueueItem]] = jsonOf[Task, List[QueueItem]]

      override def fetchQueue(server: SonarrServer): ZIO[Any, Throwable, List[QueueItem]] =
        http4sClient.runRequest {
          val uri = Uri
            .unsafeFromString(s"${server.baseUrl}/api/queue")
            .withQueryParam("apikey", server.apiKey)

          Request[Task](Method.GET, uri, headers = Headers.of(Header("Accept", "application/json")))
        } { response =>
          response.status match {
            case Status.Ok => response.as[List[QueueItem]]
            case Status.Unauthorized => ZIO.fail(InvalidApiKeyAppError())
            case _ => ZIO.fail(new IllegalStateException(s"Unknown response ${response.status}"))
          }
        }
    }
  }

  object > extends Service[MediaClient] {
    override def fetchQueue(server: SonarrServer): ZIO[MediaClient, Throwable, List[QueueItem]] =
      ZIO.accessM(_.mediaClient.fetchQueue(server))
  }

}