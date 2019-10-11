package ca.venasse.show_sync.clients

import cats.effect.Resource
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Request, Response}
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext

trait Http4sClient {

  val http4sClient: Http4sClient.Service[Any]

}

object Http4sClient {

  trait Service[-R] {
    def runRequest[T](req: Request[Task])(handler: Response[Task] => Task[T]): RIO[R, T]
  }

  trait Live extends Http4sClient {

    implicit val runtime: Runtime[Any]
    val blockingEC: ExecutionContext

    private lazy val _http4sClient: Resource[Task, Client[Task]] = BlazeClientBuilder[Task](blockingEC).resource

    override val http4sClient: Service[Any] = new Service[Any] {

      override def runRequest[T](req: Request[Task])(handler: Response[Task] => Task[T]): RIO[Any, T] =
        _http4sClient.use(_.fetch(req)(handler))
    }
  }

}