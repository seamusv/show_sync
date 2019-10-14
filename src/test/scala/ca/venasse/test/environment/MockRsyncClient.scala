package ca.venasse.test.environment

import ca.venasse.show_sync.clients.RsyncClient
import ca.venasse.show_sync.clients.RsyncClient.{Request, Response}
import zio.ZIO
import zio.test.mock._

trait MockRsyncClient extends RsyncClient {
  override val rsyncClient: MockRsyncClient.Service[Any]
}

object MockRsyncClient {

  trait Service[R] extends RsyncClient.Service[R]

  object Service {

    object runRequest extends Method[Request, Response]

  }

  implicit val mocakable: Mockable[MockRsyncClient] = (mock: Mock) =>
    new MockRsyncClient {
      override val rsyncClient: Service[Any] = new Service[Any] {
        override def runRequest(request: Request): ZIO[Any, Throwable, Response] =
          mock(Service.runRequest, request)
      }
    }
}