package ca.venasse.show_sync.clients

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import ca.venasse.show_sync.config.{LocalSettings, RSyncSettings}
import ca.venasse.show_sync.domain.{FetchStatus, Listing}
import com.github.fracpete.rsync4j.RSync
import zio.ZIO
import zio.blocking.Blocking

import scala.collection.JavaConverters._
import scala.sys.process.Process

trait SyncClient {

  val syncClient: SyncClient.Service[Any]

}

object SyncClient {

  trait Service[R] {
    def fetchListing(rootPath: String): ZIO[R, Throwable, Listing]

    def fetchFile(source: String): ZIO[R, Throwable, FetchStatus]

    def isCompleted(rootPath: String): ZIO[R, Throwable, Boolean]

    def mkLocalDir(destination: String): ZIO[R, Throwable, Boolean]

    def moveLocalDir(source: String): ZIO[R, Throwable, Boolean]

    def unrar(rootPath: String): ZIO[R, Throwable, String]
  }

  object > extends Service[SyncClient] {
    override def fetchListing(rootPath: String): ZIO[SyncClient, Throwable, Listing] =
      ZIO.accessM(_.syncClient.fetchListing(rootPath))

    override def fetchFile(source: String): ZIO[SyncClient, Throwable, FetchStatus] =
      ZIO.accessM(_.syncClient.fetchFile(source))

    override def isCompleted(rootPath: String): ZIO[SyncClient, Throwable, Boolean] =
      ZIO.accessM(_.syncClient.isCompleted(rootPath))

    override def mkLocalDir(destination: String): ZIO[SyncClient, Throwable, Boolean] =
      ZIO.accessM(_.syncClient.mkLocalDir(destination))

    override def moveLocalDir(source: String): ZIO[SyncClient, Throwable, Boolean] =
      ZIO.accessM(_.syncClient.moveLocalDir(source))

    override def unrar(rootPath: String): ZIO[SyncClient, Throwable, String] =
      ZIO.accessM(_.syncClient.unrar(rootPath))
  }

  trait Live extends SyncClient {

    val rsyncSettings: RSyncSettings
    val localSettings: LocalSettings
    val blocking: Blocking.Service[Any]

    override val syncClient: Service[Any] = new Service[Any] {
      override def fetchListing(rootPath: String): ZIO[Any, Throwable, Listing] =
        blocking.effectBlocking {
          val rsync = new RSync()
            .source(s"${rsyncSettings.sourcePrefix}/$rootPath")
            .destination(localSettings.stagePath)
            .ignoreExisting(true)
            .recursive(true)
            .pruneEmptyDirs(true)
            .protectArgs(true)
            .dryRun(true)
            .verbose(true)
            .passwordFile(rsyncSettings.passwordFile)

          println(rsync.commandLineArgs().asScala.mkString(" "))

          val output = rsync.execute()
          if (output.hasSucceeded) {
            val (paths, files) = output.getStdOut.lines
              .toList
              .filter(_.startsWith(rootPath))
              .partition(_.endsWith("/"))
            Listing(paths, files)
          } else {
            println(output.getStdErr)
            throw new IllegalStateException(output.getStdErr)
          }
        }

      override def fetchFile(source: String): ZIO[Any, Throwable, FetchStatus] =
        blocking.blocking {
          ZIO.effect {
            val rsync = new RSync()
              .source(s"${rsyncSettings.sourcePrefix}/$source")
              .destination(s"${localSettings.stagePath}/$source")
              .archive(true)
              .ignoreExisting(true)
              .pruneEmptyDirs(true)
              .protectArgs(true)
              .verbose(true)
              .passwordFile(rsyncSettings.passwordFile)

            println(rsync.commandLineArgs().asScala.mkString(" "))

            val output = rsync.execute()
            if (output.hasSucceeded) {
              FetchStatus(source)
            } else {
              println(output.getStdErr)
              throw new IllegalStateException(output.getStdErr)
            }
          }
        }

      override def isCompleted(rootPath: String): ZIO[Any, Throwable, Boolean] =
        blocking.blocking {
          ZIO.effect(new File(s"${localSettings.completedPath}/$rootPath").exists())
        }

      override def mkLocalDir(destination: String): ZIO[Any, Throwable, Boolean] =
        blocking.blocking {
          ZIO.effect(new File(s"${localSettings.stagePath}/$destination").mkdirs())
        }


      override def moveLocalDir(source: String): ZIO[Any, Throwable, Boolean] =
        blocking.blocking {
          ZIO.effect {
            val sourceFile = new File(s"${localSettings.stagePath}/$source")
            val destinationFile = new File(s"${localSettings.completedPath}/$source")

            Files.move(sourceFile.toPath, destinationFile.toPath, StandardCopyOption.REPLACE_EXISTING)
            true
          }
        }

      override def unrar(rootPath: String): ZIO[Any, Throwable, String] =
        blocking.blocking {
          ZIO.effect {
            Process("unrar x -o- *.rar", cwd = new File(s"${localSettings.stagePath}/$rootPath"))
              .lineStream
              .toList
              .mkString("\n")
          }
            .fold(_ => "", identity)
        }
    }
  }

}