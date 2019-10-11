package ca.venasse.show_sync

import ca.venasse.show_sync.domain.SonarrServer

object config {

  case class Config(
                     servers: List[SonarrServer],
                     local: LocalSettings,
                     rsync: RSyncSettings,
                   )

  case class RSyncSettings(
                            sourcePrefix: String,
                            passwordFile: String,
                          )

  case class LocalSettings(
                            stagePath: String,
                            completedPath: String,
                          )

}