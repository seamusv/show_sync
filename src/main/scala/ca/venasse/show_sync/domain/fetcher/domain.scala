package ca.venasse.show_sync.domain.fetcher

object domain {

  case class FetchStatus(source: String)

  case class Listing(paths: List[String], files: List[String])

}
