package ca.venasse.show_sync.clients

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import zio.ZIO

trait FilesystemClient {

  val filesystemClient: FilesystemClient.Service[Any]

}

object FilesystemClient {

  trait Service[R] {
    def pathExists(path: String): ZIO[R, Throwable, Boolean]

    def mkDirs(path: String): ZIO[R, Throwable, Boolean]

    def movePath(sourcePath: String, destinationPath: String): ZIO[R, Throwable, Unit]
  }

  trait Live extends FilesystemClient {

    override val filesystemClient: Service[Any] = new Service[Any] {
      override def pathExists(path: String): ZIO[Any, Throwable, Boolean] =
        ZIO.effect(new File(path).exists())

      override def mkDirs(path: String): ZIO[Any, Throwable, Boolean] =
        ZIO.effect(new File(path).mkdirs())

      override def movePath(sourcePath: String, destinationPath: String): ZIO[Any, Throwable, Unit] =
        ZIO
          .effect {
            val sourceFile = new File(sourcePath)
            val destinationFile = new File(destinationPath)

            Files.move(sourceFile.toPath, destinationFile.toPath, StandardCopyOption.REPLACE_EXISTING)
          }
          .unit
    }
  }

}