package ca.venasse.show_sync.domain

import java.util.concurrent.TimeUnit

import ca.venasse.show_sync.domain.fetcher.domain.{FetchStatus, Listing}
import ca.venasse.show_sync.domain.sonarr.domain.{QueueItem, SonarrServer}
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.duration.Duration
import zio.stream._

object Monitor {

  import fetcher._
  import sonarr._

  private def filteredFetchQueue(server: SonarrServer): ZIO[FetchClient with SonarrClient, Throwable, List[QueueItem]] =
    fetchQueue(server)
      .fold(_ => List.empty, identity)
      .flatMap { list =>
        Stream
          .fromIterable(list)
          .filter(i => i.status == "Completed" && i.protocol == "torrent")
          .filterM(item => isCompleted(item.title).map(!_))
          .runCollect
      }

  def monitorQueue(servers: List[SonarrServer]): ZIO[FetchClient with SonarrClient with Clock with Console, Throwable, Int] =
    (for {
      items <- ZIO.traverseParN(10)(servers)(filteredFetchQueue)
        .map(_.flatten.filter(i => i.status == "Completed" && i.protocol == "torrent"))
      _ <- console.putStrLn(s"Queue: $items")

      listing <- ZIO.traverseParN(10)(items)(item => fetchListing(item.title).fold(_ => Listing(List.empty, List.empty), identity))
        .map(_.foldLeft(Listing(List.empty, List.empty)) { case (acc, l) => Listing(acc.paths ++ l.paths, acc.files ++ l.files) })
      _ <- console.putStrLn(s"Listing: $listing")

      _ <- ZIO.traverse(listing.paths)(mkLocalDir)

      _ <- ZIO.traverseParN(10)(listing.files)(fetchFile(_).fold(_ => FetchStatus(""), identity))

      _ <- ZIO.traverse(items)(item => unrar(item.title))
      _ <- ZIO.traverse(items)(item => moveLocalDir(item.title))
    } yield items)
      .repeat(ZSchedule.spaced(Duration(5, TimeUnit.MINUTES)))

}
