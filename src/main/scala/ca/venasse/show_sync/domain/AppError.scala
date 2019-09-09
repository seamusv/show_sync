package ca.venasse.show_sync.domain

sealed trait AppError extends Product with Serializable

case class FetchError(message: String) extends AppError