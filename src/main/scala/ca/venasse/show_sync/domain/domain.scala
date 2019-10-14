package ca.venasse.show_sync.domain

case class FetchStatus(source: String)

case class Listing(paths: List[String], files: List[String])

case class SonarrServer(baseUrl: String, apiKey: String)

case class QueueItem(title: String, status: String, protocol: String)

sealed trait AppError extends RuntimeException {

  def message: String

  final override def getMessage: String = message

  def cause: Option[Throwable]

  final override def getCause: Throwable = cause.orNull

}

case class InvalidApiKeyAppError(cause: Option[Throwable] = None) extends AppError {
  override def message: String = "Invalid API Key"
}
