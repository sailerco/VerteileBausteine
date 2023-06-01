import hello.{GetRequest, GrpcClientGrpc, SetRequest}
import io.grpc.ManagedChannelBuilder

import java.util.logging.Logger

object GrpcClient extends App {
  val logger = Logger.getLogger(Client.getClass.getName)
  val port = 50051
  val host = "localhost"
  val channel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .asInstanceOf[ManagedChannelBuilder[_]]
    .build()

  set("IT", "Italy")
  set("DE", "Germany")
  get("IT")
  get("UK")

  def set(key: String, value: String): Unit = {
    val request = SetRequest(key, value)

    logger.info("try to set " + request.key + " with " + request.value)

    val reply = GrpcClientGrpc
      .blockingStub(channel)
      .set(request)
    logger.info(reply.toProtoString)
  }

  def get(key: String): Unit = {
    val getRequest = GetRequest(key)
    logger.info("try to get " + getRequest.key)

    val get = GrpcClientGrpc
      .blockingStub(channel)
      .get(getRequest)
    logger.info(get.toProtoString)

    if (get.getValue == "")
      logger.info(s"there is no value for key $key")
  }
}
