package ca.venasse.test.environment

import ca.venasse.show_sync.clients.Http4sClient
import org.http4s.{Request, Response}
import zio.test.mock._
import zio.{Task, _}

trait MockHttp4sClient extends Http4sClient {
  override val http4sClient: MockHttp4sClient.Service[Any]
}

object MockHttp4sClient {

  trait Service[R] extends Http4sClient.Service[R]

  object Service {

    object runRequest extends Method[(Request[Task], Response[Task] => Task[Any]), Any]

  }

  implicit val mockable: Mockable[MockHttp4sClient] = (mock: Mock) =>
    new MockHttp4sClient {
      override val http4sClient: Service[Any] = new Service[Any] {
        override def runRequest[T](req: Request[Task])(handler: Response[Task] => Task[T]): RIO[Any, T] =
          mock(Service.runRequest, (req, handler))
      }
    }

}
