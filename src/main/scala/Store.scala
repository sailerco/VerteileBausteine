import ResultProcessor.Result
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, Behaviors}
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}

import java.nio.charset.StandardCharsets

class Store(context: ActorContext[Store.Command], system: ActorSystem[_]) extends AbstractBehavior[Store.Command](context) {

  import Store._

  context.system.receptionist ! Receptionist.register(StoreService, context.self)

  private val sharding = ClusterSharding(system)

  override def onMessage(message: Store.Command): Behavior[Store.Command] = message match {
    /*case Count(replyTo) =>
      replyTo ! HandleCount(items.size)
      Behaviors.same*/
    case Get(replyTo, key) =>
      val ref: EntityRef[StoreShard.Command] = sharding.entityRefFor(StoreShard.TypeKey, hash(key))
      ref ! StoreShard.Get(replyTo, key)
      Behaviors.same
    case Set(replyTo, key, value) =>
      val ref: EntityRef[StoreShard.Command] = sharding.entityRefFor(StoreShard.TypeKey, hash(key))
      ref ! StoreShard.Set(replyTo, key, value)
      Behaviors.same
    case SetMultiple(replyTo, keyValues) =>
      keyValues.foreach(keyValues => {
        val ref: EntityRef[StoreShard.Command] = sharding.entityRefFor(StoreShard.TypeKey, hash(keyValues._1))
        ref ! StoreShard.Set(replyTo, keyValues._1, keyValues._2)
      })
      Behaviors.same
  }

  def hash(key: Seq[Byte]): String = {
    (key.hashCode() % 10).toString
  }
  private def convertToString(value: Seq[Byte]): String = {
    new String(value.toArray, StandardCharsets.UTF_8)
  }
}

object Store {

  sealed trait Command

  val StoreService: ServiceKey[Command] = ServiceKey[Command]("StoreService")

  //case class Count(replyTo: ActorRef[Result]) extends Command

  case class Get(replyTo: ActorRef[Result], key: Seq[Byte]) extends Command

  case class Set(replyTo: ActorRef[Result], key: Seq[Byte], value: Seq[Byte]) extends Command

  case class SetMultiple(replyTo: ActorRef[Result], keyValues: List[(Seq[Byte], Seq[Byte])]) extends Command

  def apply(system: ActorSystem[_]): Behavior[Store.Command] = {
    Behaviors.setup { context =>
      new Store(context, system)
    }
  }
}