import akka.Done
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

class HttpServer(store: ActorRef[Store.Command]) {
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "HttpServer")
  implicit val executionContext: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler

  final case class KeyValue(key: String, value: String)
  final case class Key(key: String)

  implicit val keyFormat: RootJsonFormat[Key] = jsonFormat1(Key)
  implicit val keyValueFormat: RootJsonFormat[KeyValue] = jsonFormat2(KeyValue)

  def setKeyValue(request: KeyValue): Future[Done] = {
    store.ask(reply => Store.Set(reply, getBytesOfValue(request.key), getBytesOfValue(request.value)))(timeout, scheduler)
    Future {
      Done
    }
  }

  def getValue(request: String): Future[Option[KeyValue]] = {
    val getValue = store.ask(reply => Store.Get(reply, getBytesOfValue(request)))(timeout, scheduler)
    val result = Await.result(getValue, 3.seconds)
    result match {
      case ResultProcessor.HandleGet(key, value) => Future.successful(Option(KeyValue(convertToString(key), convertToString(value))))
      case ResultProcessor.HandleError(_) => Future.successful(None)
    }
  }

  val route: Route =
    concat(
      get {
        pathPrefix("get" / Segment) { id =>
          val maybeValue: Future[Option[KeyValue]] = getValue(id)
          onSuccess(maybeValue) {
            case Some(keyValue) => complete(keyValue.value)
            case None => complete(StatusCodes.NotFound, "404 ERROR - Key was not found")
          }
        }
      },
      post {
        path("set") {
          entity(as[KeyValue]) { keyValue =>
            val saved: Future[Done] = setKeyValue(keyValue)
            onSuccess(saved) { _ =>
              complete("key-value-pair created")
            }
          }
        }
      }
    )
  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(route)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

  private def convertToString(value: Seq[Byte]): String = {
    new String(value.toArray, StandardCharsets.UTF_8)
  }
  private def getBytesOfValue(value: String): Array[Byte] = {
    value.getBytes("UTF-8")
  }
}