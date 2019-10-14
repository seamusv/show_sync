package queue

import ca.venasse.show_sync.clients.Http4sClient
import ca.venasse.show_sync.domain.{InvalidApiKeyAppError, MediaClient, QueueItem, SonarrServer}
import ca.venasse.test.environment.MockHttp4sClient
import io.circe.Encoder
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityEncoder, MediaType, Request, Response, Status}
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.mock.MockSpec

object mediaClientTests {

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  def makeEnvironment(mockHttp4sClient: MockHttp4sClient): MediaClient.Live = new MediaClient.Live {
    override val http4sClient: Http4sClient.Service[Any] = mockHttp4sClient.http4sClient
  }

  def uriAssertion(uri: String): Assertion[(Request[Task], Response[Task] => Task[Any])] =
    assertion[(Request[Task], Response[Task] => Task[Any])]("requestUri")(Render.param(uri)) { case (request, _) => request.uri.toString() == uri }

  val successfulQueries: Spec[Any, Throwable, String, Either[TestFailure[Nothing], TestSuccess[Unit]]] = suite("Success Suite")(
    testM("Expected URI format for API query") {
      val program = MediaClient.>.fetchCompletedTorrents(SonarrServer("sonarr.server", "apiKey"))
      val expect: Managed[Nothing, MockHttp4sClient] =
        MockSpec.expectM(MockHttp4sClient.Service.runRequest)(uriAssertion("sonarr.server/api/queue?apikey=apiKey")) { case (_, handler) =>
          val response = Response[Task](status = Status.Ok).withEntity(List.empty[QueueItem])
          handler(response)
        }
      val result = program.provideManaged(expect.map(makeEnvironment))

      assertM(result, equalTo(List.empty[String]))
    },

    testM("Filter queue results for torrents") {
      val program = MediaClient.>.fetchCompletedTorrents(SonarrServer("sonarr.server", "apiKey"))
      val expect: Managed[Nothing, MockHttp4sClient] =
        MockSpec.expectM(MockHttp4sClient.Service.runRequest)(anything) { case (_, handler) =>
          val response = Response[Task](status = Status.Ok).withEntity(List(
            QueueItem(title = "foo", status = "Completed", protocol = "torrent"),
            QueueItem(title = "bar", status = "Completed", protocol = "nzbget"),
            QueueItem(title = "baz", status = "Completed", protocol = "torrent"),
          ))
          handler(response)
        }
      val result = program.provideManaged(expect.map(makeEnvironment))

      assertM(result, equalTo(List("foo", "baz")))
    },
  )

  val failureQueries: Spec[Any, Nothing, String, Either[TestFailure[Nothing], TestSuccess[Unit]]] = suite("Failed Suite")(
    testM("Not a JSON document") {
      val program = MediaClient.>.fetchCompletedTorrents(SonarrServer("sonarr.server", "apiKey"))
      val expect: Managed[Nothing, MockHttp4sClient] =
        MockSpec.expectM(MockHttp4sClient.Service.runRequest)(anything) { case (_, handler) =>
          val response = Response[Task](status = Status.Ok).withEntity("Welcome to the Jungle!").withContentType(`Content-Type`(MediaType.text.plain))
          handler(response)
        }
      val result = program.provideManaged(expect.map(makeEnvironment)).either.map(_.swap.map(_.getMessage))

      assertM(result, isRight(startsWith("Invalid message body")))
    },

    testM("Invalid API key") {
      val program = MediaClient.>.fetchCompletedTorrents(SonarrServer("sonarr.server", "apiKey"))
      val expect: Managed[Nothing, MockHttp4sClient] =
        MockSpec.expectM(MockHttp4sClient.Service.runRequest)(anything) { case (_, handler) =>
          val response = Response[Task](status = Status.Unauthorized).withEntity(Map("error" -> "Unauthorized"))
          handler(response)
        }
      val result = program.provideManaged(expect.map(makeEnvironment)).either

      assertM(result, isLeft(equalTo[Throwable](InvalidApiKeyAppError())))
    },
  )

}

import queue.mediaClientTests._

object MediaClientSpec extends DefaultRunnableSpec(
  suite("Media Client")(
    successfulQueries,
    failureQueries,
  )
)