package ca.venasse.show_sync.domain

import ca.venasse.show_sync.clients.RsyncClient.Request
import ca.venasse.show_sync.clients.{ArchiveClient, FilesystemClient, RsyncClient}
import ca.venasse.show_sync.config.{LocalSettings, RSyncSettings}
import zio.ZIO
import zio.blocking.Blocking

trait SyncClient {

  val syncClient: SyncClient.Service[Any]

}

object SyncClient {

  trait Service[R] {
    def fetchListing(rootPath: String): ZIO[R, Throwable, Listing]

    def fetchFile(source: String): ZIO[R, Throwable, FetchStatus]

    def isCompleted(rootPath: String): ZIO[R, Throwable, Boolean]

    def mkLocalDir(destination: String): ZIO[R, Throwable, Boolean]

    def moveLocalDir(source: String): ZIO[R, Throwable, Unit]

    def unrar(rootPath: String): ZIO[R, Throwable, String]
  }

  object > extends Service[SyncClient] {
    override def fetchListing(rootPath: String): ZIO[SyncClient, Throwable, Listing] =
      ZIO.accessM(_.syncClient.fetchListing(rootPath))

    override def fetchFile(source: String): ZIO[SyncClient, Throwable, FetchStatus] =
      ZIO.accessM(_.syncClient.fetchFile(source))

    override def isCompleted(rootPath: String): ZIO[SyncClient, Throwable, Boolean] =
      ZIO.accessM(_.syncClient.isCompleted(rootPath))

    override def mkLocalDir(destination: String): ZIO[SyncClient, Throwable, Boolean] =
      ZIO.accessM(_.syncClient.mkLocalDir(destination))

    override def moveLocalDir(source: String): ZIO[SyncClient, Throwable, Unit] =
      ZIO.accessM(_.syncClient.moveLocalDir(source))

    override def unrar(rootPath: String): ZIO[SyncClient, Throwable, String] =
      ZIO.accessM(_.syncClient.unrar(rootPath))
  }

  trait Live extends SyncClient {

    val rsyncSettings: RSyncSettings
    val localSettings: LocalSettings

    val archiveClient: ArchiveClient.Service[Any]
    val blocking: Blocking.Service[Any]
    val filesystemClient: FilesystemClient.Service[Any]
    val rsyncClient: RsyncClient.Service[Any]

    override val syncClient: Service[Any] = new Service[Any] {
      override def fetchListing(rootPath: String): ZIO[Any, Throwable, Listing] =
        rsyncClient
          .runRequest(
            Request(
              source = s"${rsyncSettings.sourcePrefix}/$rootPath",
              destination = localSettings.stagePath,
              dryRun = true,
              recursive = true
            )
          )
          .flatMap { response =>
            response.status match {
              case RsyncClient.Success =>
                ZIO.succeed {
                  val (paths, files) = response.body.lines
                    .toList
                    .filter(_.startsWith(rootPath))
                    .partition(_.endsWith("/"))
                  files.foreach(println)
                  Listing(paths, files)
                }

              case RsyncClient.Failure =>
                ZIO.fail(new IllegalStateException(response.body))
            }
          }

      override def fetchFile(source: String): ZIO[Any, Throwable, FetchStatus] =
        rsyncClient
          .runRequest(
            Request(
              source = s"${rsyncSettings.sourcePrefix}/$source",
              destination = s"${localSettings.stagePath}/$source",
              dryRun = true,
              recursive = false
            )
          )
          .flatMap { response =>
            response.status match {
              case RsyncClient.Success =>
                ZIO.succeed(FetchStatus(source))

              case RsyncClient.Failure =>
                ZIO.fail(new IllegalStateException(response.body))
            }
          }

      override def isCompleted(rootPath: String): ZIO[Any, Throwable, Boolean] =
        filesystemClient.pathExists(s"${localSettings.completedPath}/$rootPath")

      override def mkLocalDir(destination: String): ZIO[Any, Throwable, Boolean] =
        filesystemClient.mkDirs(s"${localSettings.stagePath}/$destination")

      override def moveLocalDir(source: String): ZIO[Any, Throwable, Unit] =
        filesystemClient.movePath(sourcePath = s"${localSettings.stagePath}/$source", destinationPath = s"${localSettings.completedPath}/$source")

      override def unrar(rootPath: String): ZIO[Any, Throwable, String] =
        blocking.blocking {
          archiveClient.unpack(s"${localSettings.stagePath}/$rootPath")
        }
    }
  }

}