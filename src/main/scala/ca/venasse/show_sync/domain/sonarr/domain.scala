package ca.venasse.show_sync.domain.sonarr

object domain {

  case class SonarrServer(baseUrl: String, apiKey: String)

  case class QueueItem(title: String, status: String, protocol: String)

}
