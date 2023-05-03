import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.nio.charset.StandardCharsets

class ResultProcessor(context: ActorContext[ResultProcessor.Result]) extends AbstractBehavior[ResultProcessor.Result](context) {

  import ResultProcessor._

  override def onMessage(message: ResultProcessor.Result): Behavior[ResultProcessor.Result] = message match {
    case HandleCount(keySize) =>
      context.log.info(s"The Store-Key-Values have $keySize keys")
      Behaviors.stopped
    case HandleGet(key, value) =>
      context.log.info(s"value of key ${convertToString(key)} is ${convertToString(value)}}")
      Behaviors.stopped
    case HandleSet(key, value) =>
      context.log.info(s"Set key ${convertToString(key)} to ${convertToString(value)}")
      Behaviors.stopped
    case HandleSetMultiple(keyValueList: List[(Seq[Byte], Seq[Byte])]) =>
      keyValueList.foreach { k =>
        context.log.info(s"Set key ${convertToString(k._1)} to ${convertToString(k._2)}")
      }
      Behaviors.stopped
    case HandleError(key) =>
      context.log.info(s"value of key ${convertToString(key)} not found")
      Behaviors.stopped
  }

  private def convertToString(value: Seq[Byte]): String = {
    new String(value.toArray, StandardCharsets.UTF_8)
  }
}

object ResultProcessor {
  sealed trait Result

  case class HandleCount(keySize: Int) extends Result

  case class HandleGet(key: Seq[Byte], value: Seq[Byte]) extends Result

  case class HandleSet(key: Seq[Byte], value: Seq[Byte]) extends Result

  case class HandleSetMultiple(keyValueList: List[(Seq[Byte], Seq[Byte])]) extends Result

  case class HandleError(key: Seq[Byte]) extends Result

  def apply(): Behavior[ResultProcessor.Result] = {
    Behaviors.setup { context =>
      new ResultProcessor(context)
    }
  }
}