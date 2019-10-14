package ca.venasse.show_sync.domain

import zio.ZIO
import zio.stream.Stream

object Monitor {

  def processQueues(servers: List[SonarrServer]): ZIO[SyncClient with MediaClient, Throwable, List[String]] =
    for {
      torrentTitles <- ZIO.traverseParN(10)(servers)(MediaClient.>.fetchCompletedTorrents)
        .map(_.flatten)
        .flatMap { list =>
          Stream
            .fromIterable(list)
            .filterM(item => SyncClient.>.isCompleted(item).map(!_))
            .runCollect
        }

      fileListing <- ZIO.traverseParN(10)(torrentTitles)(item => SyncClient.>.fetchListing(item).fold(_ => Listing(List.empty, List.empty), identity))
        .map(_.foldLeft(Listing(List.empty, List.empty)) { case (acc, l) => Listing(acc.paths ++ l.paths, acc.files ++ l.files) })

      _ <- ZIO.traverse(fileListing.paths)(SyncClient.>.mkLocalDir)

      _ <- ZIO.traverseParN(10)(fileListing.files)(SyncClient.>.fetchFile(_).fold(_ => FetchStatus(""), identity))

      _ <- ZIO.traverse(torrentTitles)(item => SyncClient.>.unrar(item))
      _ <- ZIO.traverse(torrentTitles)(item => SyncClient.>.moveLocalDir(item))
    } yield torrentTitles

}
