import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import hello.{GetReply, GetRequest, GrpcClientGrpc, SetReply, SetRequest}
import io.grpc.ServerBuilder

import java.nio.charset.StandardCharsets
import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class ServerImpl(system: ActorSystem[_], store: ActorRef[Store.Command]) extends GrpcClientGrpc.GrpcClient{ //trait oder object??
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = system

  override def set(request: SetRequest): Future[SetReply] = {
    val setInStore = store.ask(reply => Store.Set(reply,  request.key.getBytes("UTF-8"), request.value.getBytes("UTF-8")))
    val result = Await.result(setInStore, 3.seconds)
    Future.successful(SetReply(request.key, request.value))
/*    val resultProcessor: ActorRef[ResultProcessor.Result] = system.systemActorOf(ResultProcessor(), "result")
    store ! Store.Set(resultProcessor, request.key.getBytes("UTF-8"), request.value.getBytes("UTF-8"))
    val reply = SetReply(request.key, request.value)
    Future.successful(reply)*/
  }

  //TODO: handle Error correctly
  override def get(request: GetRequest): Future[GetReply] = {
    val getStore = store.ask(reply => Store.Get(reply, request.key.getBytes("UTF-8")))
    val result = Await.result(getStore, 3.seconds)

    result match {
      case ResultProcessor.HandleGet(key, value) => {
        val value_result = new String(result.asInstanceOf[ResultProcessor.HandleGet].value.toArray, StandardCharsets.UTF_8)
        Future.successful(GetReply(request.key, Option(value_result)))
      }
      case ResultProcessor.HandleError(key) => {Future.successful(GetReply(request.key))}
    }
  }
}

class Server(system: ActorSystem[_], store: ActorRef[Store.Command]){
  val logger = Logger.getLogger(this.getClass.getName)
  val port = 50051
  val host = "localhost"
  val service = GrpcClientGrpc.bindService(new ServerImpl(system, store), ExecutionContext.global)
  val server = ServerBuilder
    .forPort(port)
    .addService(service)
    .asInstanceOf[ServerBuilder[_]]
    .build()
    .start()

  logger.info("server started, listening on port"+port)
  server.awaitTermination()
}
