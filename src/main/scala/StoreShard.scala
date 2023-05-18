import ResultProcessor._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

class StoreShard(context: ActorContext[StoreShard.Command]) extends AbstractBehavior[StoreShard.Command](context) {

  import StoreShard._

  private val items = scala.collection.mutable.Map[Seq[Byte], Seq[Byte]]()

  def onMessage(message: StoreShard.Command): Behavior[StoreShard.Command] = message match {
    case Get(replyTo, key) =>
      items.get(key) match {
        case Some(value) => replyTo ! HandleGet(key, value)
        case None => replyTo ! HandleError(key)
      }
      Behaviors.same
    case Set(replyTo, key, value) =>
      items.addOne(key, value)
      replyTo ! HandleSet(key, items(key))
      Behaviors.same
  }
}

object StoreShard {
  sealed trait Command

  val TypeKey: EntityTypeKey[StoreShard.Command] = EntityTypeKey[StoreShard.Command]("StoreShard")

  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command

  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command

  def initSharding(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(TypeKey)(createBehavior = entityContext => StoreShard(entityContext.entityId)).withRole("StoreSharding"))
  }

  def apply(entity: String): Behavior[StoreShard.Command] = {
    Behaviors.setup { context =>
      new StoreShard(context)
    }
  }
}
