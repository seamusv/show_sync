package ca.venasse.show_sync

import ca.venasse.show_sync.clients.{MediaClient, SyncClient}
import ca.venasse.show_sync.domain.{FetchStatus, Listing, QueueItem, SonarrServer}
import zio.console.Console
import zio.stream.Stream
import zio.{ZIO, console}

object Monitor {

  private def filteredFetchQueue(server: SonarrServer): ZIO[SyncClient with MediaClient, Throwable, List[QueueItem]] =
    MediaClient.>.fetchQueue(server)
      .fold(_ => List.empty, identity)
      .flatMap { list =>
        Stream
          .fromIterable(list)
          .filter(i => i.status == "Completed" && i.protocol == "torrent")
          .filterM(item => SyncClient.>.isCompleted(item.title).map(!_))
          .runCollect
      }

  def processQueues(servers: List[SonarrServer]): ZIO[SyncClient with Console with MediaClient, Throwable, List[QueueItem]] =
    for {
      items <- ZIO.traverseParN(10)(servers)(filteredFetchQueue)
        .map(_.flatten.filter(i => i.status == "Completed" && i.protocol == "torrent"))
      _ <- console.putStrLn(s"Queue: $items")

      listing <- ZIO.traverseParN(10)(items)(item => SyncClient.>.fetchListing(item.title).fold(_ => Listing(List.empty, List.empty), identity))
        .map(_.foldLeft(Listing(List.empty, List.empty)) { case (acc, l) => Listing(acc.paths ++ l.paths, acc.files ++ l.files) })
      _ <- console.putStrLn(s"Listing: $listing")

      _ <- ZIO.traverse(listing.paths)(SyncClient.>.mkLocalDir)

      _ <- ZIO.traverseParN(10)(listing.files)(SyncClient.>.fetchFile(_).fold(_ => FetchStatus(""), identity))

      _ <- ZIO.traverse(items)(item => SyncClient.>.unrar(item.title))
      _ <- ZIO.traverse(items)(item => SyncClient.>.moveLocalDir(item.title))
    } yield items

}
