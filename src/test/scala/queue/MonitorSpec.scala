package queue

import ca.venasse.show_sync.clients.{ArchiveClient, FilesystemClient, Http4sClient, RsyncClient}
import ca.venasse.show_sync.config
import ca.venasse.show_sync.config.{LocalSettings, RSyncSettings}
import ca.venasse.show_sync.domain._
import ca.venasse.test.environment.{MockArchiveClient, MockFilesystemClient, MockHttp4sClient, MockRsyncClient}
import io.circe.Encoder
import io.circe.generic.auto._
import org.http4s.circe.jsonEncoderOf
import org.http4s.{EntityEncoder, Status}
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._
import zio.test.mock.MockSpec

object MonitorSpec {

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  val defaultRsyncSettings = RSyncSettings("sourcePrefix", "passwordFile")
  val defaultLocalSettings = LocalSettings("stagePath", "completedPath")

  val successSuite: Spec[Any, Throwable, String, Either[TestFailure[Nothing], TestSuccess[Unit]]] = suite("Success")(
    testM("Proof that the app does more than it should...") {
      val program = Monitor.processQueues(List(SonarrServer("foo", "bar")))
      val expectHttp4S: Managed[Nothing, MockHttp4sClient] =
        MockSpec.expectM(MockHttp4sClient.Service.runRequest)(anything) { case (_, handler) =>
          val response = org.http4s.Response[Task](status = Status.Ok).withEntity(List(QueueItem("foo", "Completed", "torrent")))
          handler(response)
        }

      val expectRsync: Managed[Nothing, MockRsyncClient] =
        MockSpec.expect(MockRsyncClient.Service.runRequest)(equalTo(RsyncClient.Request("sourcePrefix/foo", "stagePath", true, true)))(_ => RsyncClient.Response(RsyncClient.Success, ""))

      val expectFilesystem: Managed[Nothing, MockFilesystemClient] = (
        MockSpec.expect(MockFilesystemClient.Service.pathExists)(equalTo("completedPath/foo"))(_ => false) *>
          MockSpec.expect(MockFilesystemClient.Service.movePath)(equalTo(("stagePath/foo", "completedPath/foo")))(_ => ())
        )

      val expectArchive: Managed[Nothing, MockArchiveClient] =
        MockSpec.expect(MockArchiveClient.Service.unpack)(equalTo("stagePath/foo"))(_ => "")

      val expect: Managed[Nothing, (((MockHttp4sClient, MockRsyncClient), MockFilesystemClient), MockArchiveClient)] =
        expectHttp4S &&& expectRsync &&& expectFilesystem &&& expectArchive

      val result = program.provideManaged(expect.map { case (((mockHttpClient, mockRsyncClient), mockFilesystemClient), mockArchiveClient) =>
        new MediaClient.Live with SyncClient.Live with Blocking.Live {
          override val http4sClient: Http4sClient.Service[Any] = mockHttpClient.http4sClient
          override val rsyncSettings: config.RSyncSettings = defaultRsyncSettings
          override val localSettings: config.LocalSettings = defaultLocalSettings
          override val archiveClient: ArchiveClient.Service[Any] = mockArchiveClient.archiveClient
          override val filesystemClient: FilesystemClient.Service[Any] = mockFilesystemClient.filesystemClient
          override val rsyncClient: RsyncClient.Service[Any] = mockRsyncClient.rsyncClient
        }
      })

      assertM(result, equalTo(List("foo")))
    }
  )

}

import queue.MonitorSpec._

object MonitorSpecRunning extends DefaultRunnableSpec(
  suite("Monitor Specification")(
    successSuite,
  )
)