package ca.venasse.test.environment

import ca.venasse.show_sync.clients.ArchiveClient
import zio._

trait TestArchiveClient extends ArchiveClient {
  override val archiveClient: TestArchiveClient.Service[Any]
}

object TestArchiveClient {

  trait Service[R] extends ArchiveClient.Service[Any] {
    def updateState(state: Data): UIO[Unit]
  }

  case class Test(archiveState: Ref[Data]) extends Service[Any] {
    override def updateState(state: Data): UIO[Unit] =
      archiveState.update(_ => state).unit

    override def unpack(path: String): ZIO[Any, Throwable, String] =
      archiveState.get.flatMap(state => state.exception.fold(ZIO.effect(state.result))(ZIO.fail))
  }

  def make(state: Data): ZIO[Any, Nothing, TestArchiveClient] =
    makeTest(state).map { test =>
      new TestArchiveClient {
        override val archiveClient: Service[Any] = test
      }
    }

  def makeTest(state: Data): ZIO[Any, Nothing, Test] =
    Ref.make(state).map(Test)

  def updateState(state: Data): ZIO[TestArchiveClient, Nothing, Unit] =
    ZIO.accessM(_.archiveClient.updateState(state))

  case class Data(result: String, exception: Option[Throwable])

}