package ca.venasse.show_sync.domain
import zio.ZIO

package object fetcher extends FetchClient.Service[FetchClient] {
  override def fetchListing(rootPath: String): ZIO[FetchClient, Throwable, domain.Listing] =
    ZIO.accessM(_.fetcher.fetchListing(rootPath))

  override def fetchFile(source: String): ZIO[FetchClient, Throwable, domain.FetchStatus] =
    ZIO.accessM(_.fetcher.fetchFile(source))


  override def isCompleted(rootPath: String): ZIO[FetchClient, Throwable, Boolean] =
    ZIO.accessM(_.fetcher.isCompleted(rootPath))

  override def mkLocalDir(destination: String): ZIO[FetchClient, Throwable, Boolean] =
    ZIO.accessM(_.fetcher.mkLocalDir(destination))


  override def moveLocalDir(source: String): ZIO[FetchClient, Throwable, Boolean] =
    ZIO.accessM(_.fetcher.moveLocalDir(source))

  override def unrar(rootPath: String): ZIO[FetchClient, Throwable, String] =
    ZIO.accessM(_.fetcher.unrar(rootPath))
}
