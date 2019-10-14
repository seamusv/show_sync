package ca.venasse.show_sync.clients

import ca.venasse.show_sync.config.RSyncSettings
import com.github.fracpete.processoutput4j.output.CollectingProcessOutput
import com.github.fracpete.rsync4j.RSync
import zio.ZIO
import zio.blocking.Blocking

trait RsyncClient {

  val rsyncClient: RsyncClient.Service[Any]

}

object RsyncClient {

  trait Service[R] {

    def runRequest(request: Request): ZIO[R, Throwable, Response]

  }

  trait Live extends RsyncClient {

    val blocking: Blocking.Service[Any]
    val rsyncSettings: RSyncSettings

    override val rsyncClient: Service[Any] = new Service[Any] {

      override def runRequest(request: Request): ZIO[Any, Nothing, Response] = {
        blocking.effectBlocking {
          val rsync = new RSync()
            .source(request.source)
            .destination(request.destination)
            .archive(true)
            .dryRun(request.dryRun)
            .ignoreExisting(true)
            .pruneEmptyDirs(true)
            .protectArgs(true)
            .recursive(request.recursive)
            .verbose(true)
            .passwordFile(rsyncSettings.passwordFile)

          val output: CollectingProcessOutput = rsync.execute()
          if (output.hasSucceeded) {
            Response(Success, output.getStdOut)
          } else {
            Response(Failure, output.getStdOut)
          }
        }
          .foldCauseM(
            cause => ZIO.succeed(Response(Failure, cause.defects.map(_.getMessage).mkString("\n"))),
            ZIO.succeed
          )
      }
    }
  }

  case class Request(source: String, destination: String, dryRun: Boolean, recursive: Boolean)

  case class Response(status: Status, body: String)

  sealed trait Status

  case object Success extends Status

  case object Failure extends Status

}