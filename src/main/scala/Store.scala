import ResultProcessor.{HandleCount, HandleError, HandleGet, HandleSet, HandleSetMultiple, Result}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, Behaviors}
import akka.actor.typed.scaladsl.ActorContext

class Store(context: ActorContext[Store.Command]) extends AbstractBehavior[Store.Command](context) {

  import Store._

  context.self ! Register()

  private val items = scala.collection.mutable.Map[Seq[Byte], Seq[Byte]]()

  override def onMessage(message: Store.Command): Behavior[Store.Command] = message match {
    case Count(replyTo) =>
      replyTo ! HandleCount(items.size)
      Behaviors.same
    case Get(replyTo, key) =>
      try {
        replyTo ! HandleGet(key, items(key))
        Behaviors.same
      } catch {
        case _: NoSuchElementException =>
          replyTo ! HandleError(key)
          Behaviors.same
      }
    case Set(replyTo, key, value) =>
      items.addOne(key, value)
      replyTo ! HandleSet(key, items(key))
      Behaviors.same
    case SetMultiple(replyTo, keyValues) =>
      items.addAll(keyValues)
      replyTo ! HandleSetMultiple(keyValues)
      Behaviors.same
    case Register() =>
      context.system.receptionist ! Receptionist.register(StoreService, context.self)
      Behaviors.same
  }
}

object Store {

  sealed trait Command

  val StoreService: ServiceKey[Command] = ServiceKey[Command]("StoreService")

  case class Count(replyTo: ActorRef[Result]) extends Command

  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command

  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command

  case class SetMultiple(replyTo: ActorRef[Result], keyValues: List[(Seq[Byte], Seq[Byte])]) extends Command

  case class Register() extends Command

  def apply(): Behavior[Store.Command] = {
    Behaviors.setup { context =>
      new Store(context)
    }
  }
}