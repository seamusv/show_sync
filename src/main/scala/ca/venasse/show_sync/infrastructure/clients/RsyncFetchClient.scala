package ca.venasse.show_sync.infrastructure.clients

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import ca.venasse.show_sync.config.{LocalSettings, RSyncSettings}
import ca.venasse.show_sync.domain.fetcher.FetchClient
import ca.venasse.show_sync.domain.fetcher.domain.{FetchStatus, Listing}
import com.github.fracpete.rsync4j.RSync
import zio.ZIO
import zio.blocking.Blocking

import scala.collection.JavaConverters._
import scala.sys.process.Process

trait RsyncFetchClient extends FetchClient with Blocking {

  protected val rsyncSettings: RSyncSettings
  protected val localSettings: LocalSettings

  override val fetcher: FetchClient.Service[Any] = new FetchClient.Service[Any] {

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
