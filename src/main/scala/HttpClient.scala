import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat2}
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

object HttpClient {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080"))


    final case class KeyValue(key: String, value: String)
    final case class Key(key: String)
    implicit val keyFormat: RootJsonFormat[Key] = jsonFormat1(Key)
    implicit val keyValueFormat: RootJsonFormat[KeyValue] = jsonFormat2(KeyValue)

    responseFuture
      .onComplete {
        case Success(res) =>
          println(res)
          Unmarshal(res.entity).to[KeyValue].onComplete{
            case Success(item)=> println(item.value)
          }
        case Failure(_)   => sys.error("something wrong")
      }
  }
}