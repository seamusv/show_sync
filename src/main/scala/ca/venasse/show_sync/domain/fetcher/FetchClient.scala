package ca.venasse.show_sync.domain.fetcher

import ca.venasse.show_sync.domain.fetcher.domain.{FetchStatus, Listing}
import zio.ZIO

trait FetchClient {

  val fetcher: FetchClient.Service[Any]

}

object FetchClient {

  trait Service[R] {

    def fetchListing(rootPath: String): ZIO[R, Throwable, Listing]

    def fetchFile(source: String): ZIO[R, Throwable, FetchStatus]

    def isCompleted(rootPath: String): ZIO[R, Throwable, Boolean]

    def mkLocalDir(destination: String): ZIO[R, Throwable, Boolean]

    def moveLocalDir(source: String): ZIO[R, Throwable, Boolean]

    def unrar(rootPath: String): ZIO[R, Throwable, String]
  }

}