import hello.{GetRequest, GrpcClientGrpc, SetRequest}
import io.grpc.ManagedChannelBuilder

import java.util.logging.Logger

trait StoreClient {
  def get(key: String, action: Option[String] => Unit): Unit
  def set(key: String, value: String): Unit
}

class GrpcClient extends StoreClient {
  val logger = Logger.getLogger(Client.getClass.getName)
  val port = 50051
  val host = "localhost"
  val channel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .asInstanceOf[ManagedChannelBuilder[_]]
    .build()

  new Demo(this)

  override def set(key: String, value: String): Unit = {
    val request = SetRequest(key, value)
    logger.info("try to set " + request.key + " with " + request.value)
    val reply = GrpcClientGrpc
      .blockingStub(channel)
      .set(request)
    println("key-value-pair was created")
    logger.info(reply.value)
  }
  override def get(key: String, action: Option[String] => Unit): Unit = {
    val getRequest = GetRequest(key)
    val get = GrpcClientGrpc
      .blockingStub(channel)
      .get(getRequest)

    if (get.getValue == "")
      action(None)
    else
      action(Option(get.value.get))
  }
}