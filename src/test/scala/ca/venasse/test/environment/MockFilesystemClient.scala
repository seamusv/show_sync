package ca.venasse.test.environment

import ca.venasse.show_sync.clients.FilesystemClient
import zio.ZIO
import zio.test.mock._

trait MockFilesystemClient extends FilesystemClient {
  override val filesystemClient: MockFilesystemClient.Service[Any]
}

object MockFilesystemClient {

  trait Service[R] extends FilesystemClient.Service[R]

  object Service {

    object pathExists extends Method[String, Boolean]

    object mkDirs extends Method[String, Boolean]

    object movePath extends Method[(String, String), Unit]

  }

  implicit val mockable: Mockable[MockFilesystemClient] = (mock: Mock) =>
    new MockFilesystemClient {
      override val filesystemClient: Service[Any] = new Service[Any] {
        override def pathExists(path: String): ZIO[Any, Throwable, Boolean] =
          mock(Service.pathExists, path)

        override def mkDirs(path: String): ZIO[Any, Throwable, Boolean] =
          mock(Service.mkDirs, path)

        override def movePath(sourcePath: String, destinationPath: String): ZIO[Any, Throwable, Unit] =
          mock(Service.movePath, (sourcePath, destinationPath))
      }
    }
}