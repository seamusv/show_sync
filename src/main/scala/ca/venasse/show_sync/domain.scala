package ca.venasse.show_sync

object domain {

  case class FetchStatus(source: String)

  case class Listing(paths: List[String], files: List[String])

  case class SonarrServer(baseUrl: String, apiKey: String)

  case class QueueItem(title: String, status: String, protocol: String)

}
