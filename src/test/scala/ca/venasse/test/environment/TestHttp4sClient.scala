package ca.venasse.test.environment

import java.io.EOFException

import ca.venasse.show_sync.clients.Http4sClient
import org.http4s.{Request, Response}
import zio._

trait TestHttp4sClient extends Http4sClient {
  override val http4sClient: TestHttp4sClient.Service[Any]
}

object TestHttp4sClient {

  trait Service[R] extends Http4sClient.Service[R] {
    def feedResponses(responses: Response[Task]*): UIO[Unit]
    def requests: UIO[List[Request[Task]]]
  }

  case class Test(httpState: Ref[Data]) extends TestHttp4sClient.Service[Any] {

    override def runRequest[T](req: Request[Task])(handler: Response[Task] => Task[T]): RIO[Any, T] =
      for {
        response <- httpState.get.flatMap {
          state =>
            IO.fromOption(state.responses.headOption)
              .mapError(_ => new EOFException("There is no more responses left to return"))
        }
        _ <- httpState.update { state =>
          Data(state.requests :+ req, state.responses.tail)
        }
        value <- handler(response)
      } yield value

    def feedResponses(responses: Response[Task]*): UIO[Unit] =
      httpState
        .update { state =>
          Data(state.requests, state.responses ++ responses)
        }
        .unit

    def requests: UIO[List[Request[Task]]] =
      httpState.get.map(_.requests)
  }

  def make(state: Data = Data()): ZIO[Any, Nothing, TestHttp4sClient] =
    makeTest(state).map { test =>
      new TestHttp4sClient {
        override val http4sClient: Service[Any] = test
      }
    }

  def makeTest(state: Data): ZIO[Any, Nothing, Test] =
    Ref.make(state).map(Test)

  def feedResponses(responses: Response[Task]*): ZIO[TestHttp4sClient, Nothing, Unit] =
    ZIO.accessM(_.http4sClient.feedResponses(responses: _*))

  def requests: ZIO[TestHttp4sClient, Nothing, List[Request[Task]]] =
    ZIO.accessM(_.http4sClient.requests)

  case class Data(requests: List[Request[Task]] = List.empty, responses: List[Response[Task]] = List.empty)

}