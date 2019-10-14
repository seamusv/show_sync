package ca.venasse.show_sync.clients

import java.io.File

import zio.ZIO

import scala.sys.process.Process

trait ArchiveClient {

  val archiveClient: ArchiveClient.Service[Any]

}

object ArchiveClient {

  trait Service[R] {
    def unpack(path: String): ZIO[R, Throwable, String]
  }

  trait Live extends ArchiveClient {
    override val archiveClient: Service[Any] = new Service[Any] {
      override def unpack(path: String): ZIO[Any, Throwable, String] =
        ZIO.effect {
          Process("unrar x -o- *.rar", cwd = new File(path))
            .lineStream
            .toList
            .mkString("\n")
        }
    }
  }

  object > extends Service[ArchiveClient] {
    override def unpack(path: String): ZIO[ArchiveClient, Throwable, String] =
      ZIO.accessM(_.archiveClient.unpack(path))
  }

}