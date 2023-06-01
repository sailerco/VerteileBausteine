import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.util.Timeout
import hello._
import io.grpc
import io.grpc.{ServerBuilder, ServerServiceDefinition}

import java.nio.charset.StandardCharsets
import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class ServerImpl(system: ActorSystem[_], store: ActorRef[Store.Command]) extends GrpcClientGrpc.GrpcClient {
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler

  override def set(request: SetRequest): Future[SetReply] = {
    val setInStore = store.ask(reply => Store.Set(reply, getBytesOfValue(request.key), getBytesOfValue(request.value)))
    val result = Await.result(setInStore, 3.seconds)
    val keyValues = result.asInstanceOf[ResultProcessor.HandleSet]
    Future.successful(SetReply(convertToString(keyValues.key), convertToString(keyValues.value)))
  }

  override def get(request: GetRequest): Future[GetReply] = {
    val getStore = store.ask(reply => Store.Get(reply, getBytesOfValue(request.key)))
    val result = Await.result(getStore, 3.seconds)

    result match {
      case ResultProcessor.HandleGet(key, value) => Future.successful(GetReply(convertToString(key), Option(convertToString(value))))
      case ResultProcessor.HandleError(key) => Future.successful(GetReply(convertToString(key)))
    }
  }

  private def convertToString(value: Seq[Byte]): String = {
    new String(value.toArray, StandardCharsets.UTF_8)
  }

  private def getBytesOfValue(value: String): Array[Byte] = {
    value.getBytes("UTF-8")
  }
}

class Server(system: ActorSystem[_], store: ActorRef[Store.Command]) {
  val logger: Logger = Logger.getLogger(this.getClass.getName)
  val port = 50051
  val host = "localhost"
  val service: ServerServiceDefinition = GrpcClientGrpc.bindService(new ServerImpl(system, store), ExecutionContext.global)
  val server: grpc.Server = ServerBuilder
    .forPort(port)
    .addService(service)
    .asInstanceOf[ServerBuilder[_]]
    .build()
    .start()

  logger.info("server started, listening on port" + port)
  //server.awaitTermination()
}
