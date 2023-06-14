import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import spray.json.DefaultJsonProtocol.jsonFormat2
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.model.StatusCodes
import spray.json._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class HttpClient extends StoreClient {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  final case class KeyValue(key: String, value: String)
  implicit val keyValueFormat: RootJsonFormat[KeyValue] = jsonFormat2(KeyValue)

  new Demo(this)

  override def get(key: String, action: Option[String] => Unit): Unit = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = s"http://localhost:8080/get/$key"))
    responseFuture.onComplete{
      case Success(value) =>
        value.status match {
          case StatusCodes.OK =>
            Unmarshaller.stringUnmarshaller(value.entity).onComplete(value => action(Option(value.get)))
          case _ => action(None)
        }
      case Failure(_) => sys.error("something went wrong")
    }
  }

  override def set(key: String, value: String): Unit = {
    val entity = ContentTypes.`application/json`
    val keyValuePair = KeyValue(key, value)
    val data = keyValuePair.toJson.prettyPrint
    val req = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://localhost:8080/set",
      entity = HttpEntity(entity, data)
    )
     val responseFuture: Future[HttpResponse] = Http().singleRequest(req)

    responseFuture.onComplete{
      case Success(value) =>
        println(value)
        Unmarshaller.stringUnmarshaller(value.entity).onComplete{
          case Success(value) => println(value)
        }
      case Failure(_) => sys.error("something went wrong")
    }
  }
}