package ca.venasse.test.environment

import java.io.EOFException

import ca.venasse.show_sync.clients.RsyncClient
import ca.venasse.show_sync.clients.RsyncClient.{Request, Response}
import zio._

trait TestRsyncClient extends RsyncClient {
  override val rsyncClient: TestRsyncClient.Service[Any]
}

object TestRsyncClient {

  trait Service[R] extends RsyncClient.Service[R] {
    def feedResponses(responses: Response*): UIO[Unit]
    def responses: UIO[List[Response]]
  }

  case class Test(rsyncState: Ref[Data]) extends Service[Any] {
    override def runRequest(request: Request): ZIO[Any, Throwable, Response] =
      for {
        response <- rsyncState.get.flatMap {
          state =>
            IO.fromOption(state.responses.headOption)
              .mapError(_ => new EOFException("There is no more responses left to return"))
        }
        _ <- rsyncState.update { state =>
          Data(state.requests :+ request, state.responses.tail)
        }
      } yield response


    override def feedResponses(responses: Response*): UIO[Unit] =
      rsyncState
        .update { state =>
          Data(state.requests, state.responses ++ responses)
        }
        .unit

    override def responses: UIO[List[Response]] =
      rsyncState.get.map(_.responses)
  }

  def make(state: Data = Data(List.empty, List.empty)): ZIO[Any, Nothing, TestRsyncClient] =
    makeTest(state).map{ test =>
      new TestRsyncClient {
        override val rsyncClient: Service[Any] = test
      }
    }

  def makeTest(state: Data): ZIO[Any, Nothing, Test] =
    Ref.make(state).map(Test)

  def feedResponses(responses: Response*): ZIO[TestRsyncClient, Nothing, Unit] =
    ZIO.accessM(_.rsyncClient.feedResponses(responses: _*))

  def responses: ZIO[TestRsyncClient, Nothing, List[Response]] =
    ZIO.accessM(_.rsyncClient.responses)

  case class Data(requests: List[Request], responses: List[Response])

}