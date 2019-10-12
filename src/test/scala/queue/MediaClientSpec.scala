package queue

import ca.venasse.show_sync.clients.MediaClient
import ca.venasse.show_sync.domain.{InvalidApiKeyAppError, QueueItem, SonarrServer}
import ca.venasse.test.environment.TestHttp4sClient
import io.circe.Encoder
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityEncoder, MediaType, Response, Status}
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

object mediaClientTests {

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  val successfulQueries: Spec[MediaClient with TestHttp4sClient, Throwable, String, Either[TestFailure[Nothing], TestSuccess[Unit]]] = suite("Success Suite")(
    testM("Empty queue") {
      for {
        _ <- TestHttp4sClient.feedResponses(Response[Task](status = Status.Ok).withEntity(List.empty[QueueItem]))
        queuedItems <- MediaClient.>.fetchQueue(SonarrServer("http://sonarr.server", "mySecretKey"))
        requests <- TestHttp4sClient.requests
      } yield assert(queuedItems.length, equalTo(0)) &&
        assert(requests.headOption.map(_.uri.toString()), isSome(equalTo("http://sonarr.server/api/queue?apikey=mySecretKey")))
    },

    testM("Single torrent") {
      for {
        _ <- TestHttp4sClient.feedResponses(Response[Task](status = Status.Ok).withEntity(List(QueueItem(title = "foo", status = "Completed", protocol = "torrent"))))
        queuedItems <- MediaClient.>.fetchQueue(SonarrServer("http://sonarr.server", "mySecretKey"))
      } yield assert(queuedItems.length, equalTo(1))
    },
  )

  val failureQueries: Spec[MediaClient with TestHttp4sClient, Throwable, String, Either[TestFailure[Nothing], TestSuccess[Unit]]] = suite("Failed Suite")(
    testM("Not a JSON document") {
      for {
        _ <- TestHttp4sClient.feedResponses(Response[Task](status = Status.Ok).withEntity("Welcome to the Jungle!").withContentType(`Content-Type`(MediaType.text.plain)))
        expectedError <- MediaClient.>.fetchQueue(SonarrServer("http://sonarr.server", "mySecretKey")).either.map(_.swap.map(_.getMessage))
      } yield assert(expectedError, isRight(startsWith("Invalid message body")))
    },

    testM("Invalid API key") {
      for {
        _ <- TestHttp4sClient.feedResponses(Response[Task](status = Status.Unauthorized).withEntity(Map("error" -> "Unauthorized")))
        result <- MediaClient.>.fetchQueue(SonarrServer("http://sonarr.server", "mySecretKey")).either
      } yield assert(result, isLeft(equalTo[Throwable](InvalidApiKeyAppError())))
    },
  )

}

import queue.mediaClientTests._

object MediaClientSpec extends DefaultRunnableSpec(
  suite("Media Client")(
    successfulQueries,
    failureQueries,
  ).provideManaged(
    Managed.fromEffect {
      for {
        httpClient <- TestHttp4sClient.make()
      } yield new MediaClient.Live with TestHttp4sClient {
        override val http4sClient: TestHttp4sClient.Service[Any] = httpClient.http4sClient
      }
    }
  )
)