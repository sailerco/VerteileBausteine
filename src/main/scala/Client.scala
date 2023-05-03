import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

class Client(val store: ActorRef[Store.Command], context: ActorContext[Client.Command]) extends AbstractBehavior[Client.Command](context) {

  import Client._

  context.self ! Client.Register()

  override def onMessage(message: Client.Command): Behavior[Client.Command] = message match {
    case Count() =>
      val result = context.spawnAnonymous(ResultProcessor())
      store ! Store.Count(result)
      Behaviors.same
    case Get(key) =>
      val result = context.spawnAnonymous(ResultProcessor())
      store ! Store.Get(result, getBytesOfValue(key))
      Behaviors.same
    case Set(key, value) =>
      val result = context.spawnAnonymous(ResultProcessor())
      store ! Store.Set(result, getBytesOfValue(key), getBytesOfValue(value))
      Behaviors.same
    case SetMultiple(keyValueList) =>
      val result = context.spawnAnonymous(ResultProcessor())
      val keyValuesMap = keyValueList.map { key =>
        (getBytesOfValue(key._1).toSeq,
          getBytesOfValue(key._2).toSeq)
      }
      store ! Store.SetMultiple(result, keyValuesMap)
      Behaviors.same
    case Register() =>
      context.system.receptionist ! Receptionist.register(ClientService, context.self)
      Behaviors.same
  }

  private def getBytesOfValue(value: String): Array[Byte] = {
    value.getBytes("UTF-8")
  }
}

object Client {

  sealed trait Command

  val ClientService: ServiceKey[Command] = ServiceKey[Command]("ClientService")

  case class Count() extends Command

  case class Get(key: String) extends Command

  case class Set(key: String, value: String) extends Command

  case class SetMultiple(keyValueList: List[(String, String)]) extends Command

  case class Register() extends Command

  def apply(store: ActorRef[Store.Command]): Behavior[Client.Command] = {
    Behaviors.setup { context =>
      new Client(store, context)
    }
  }
}
