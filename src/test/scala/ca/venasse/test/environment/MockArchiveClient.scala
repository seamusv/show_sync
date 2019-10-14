package ca.venasse.test.environment

import ca.venasse.show_sync.clients.ArchiveClient
import zio.ZIO
import zio.test.mock._

trait MockArchiveClient extends ArchiveClient {

}

object MockArchiveClient {

  trait Service[R] extends ArchiveClient.Service[R]

  object Service {

    object unpack extends Method[String, String]

  }

  implicit val mockable: Mockable[MockArchiveClient] = (mock: Mock) =>
    new MockArchiveClient {
      override val archiveClient: Service[Any] = new Service[Any] {
        override def unpack(path: String): ZIO[Any, Throwable, String] = mock(Service.unpack, path)
      }
    }
}