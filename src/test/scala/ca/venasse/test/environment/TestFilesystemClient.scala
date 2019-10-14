package ca.venasse.test.environment

import ca.venasse.show_sync.clients.FilesystemClient
import zio._

trait TestFilesystemClient extends FilesystemClient {
  override val filesystemClient: TestFilesystemClient.Service[Any]
}

object TestFilesystemClient {

  trait Service[R] extends FilesystemClient.Service[R] {
    def updateState(state: Data): UIO[Unit]
  }

  case class Test(filesystemState: Ref[Data]) extends Service[Any] {
    override def updateState(data: Data): UIO[Unit] =
      filesystemState.update(_ => data).unit

    override def pathExists(path: String): ZIO[Any, Throwable, Boolean] =
      filesystemState.get
        .flatMap(state => state.exception.fold(ZIO.effect(state.pathExistsSuccess))(ZIO.fail))

    override def mkDirs(path: String): ZIO[Any, Throwable, Boolean] =
      filesystemState.get
        .flatMap(state => state.exception.fold(ZIO.effect(state.mkdirsSuccess))(ZIO.fail))

    override def movePath(sourcePath: String, destinationPath: String): ZIO[Any, Throwable, Unit] =
      filesystemState.get
        .flatMap(state => state.exception.fold(ZIO.effect(()))(ZIO.fail))

  }

  def make(state: Data): ZIO[Any, Nothing, TestFilesystemClient] =
    makeTest(state).map { test =>
      new TestFilesystemClient {
        override val filesystemClient: Service[Any] = test
      }
    }

  def makeTest(state: Data): ZIO[Any, Nothing, Test] =
    Ref.make(state).map(Test)

  def updateState(state: Data): ZIO[TestFilesystemClient, Nothing, Unit] =
    ZIO.accessM(_.filesystemClient.updateState(state))

  case class Data(pathExistsSuccess: Boolean, mkdirsSuccess: Boolean, exception: Option[Throwable])

}