import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.util.Scanner
import java.io

class FileReader(context: ActorContext[FileReader.Message], batchSize:Int) extends AbstractBehavior[FileReader.Message](context) {

  import FileReader._

  override def onMessage(msg: Message): Behavior[Message] = msg match {
    case File(filename, replyTo) =>
      val scanner = new Scanner(new io.FileReader(filename))
      while(scanner.hasNext()) {
        val line = LazyList.continually(scanner.nextLine().split(",")).takeWhile(_ => scanner.hasNext()).take(batchSize)
        val tupleList = line.map(entry => (entry(0), entry(1))).toList
        replyTo ! Client.SetMultiple(tupleList)
      }
      scanner.close()
      Behaviors.same
  }
}

object FileReader {
  sealed trait Message

  case class File(filename: String, client: ActorRef[Client.Command]) extends Message

  def apply(): Behavior[FileReader.Message] = {
    Behaviors.setup { context =>
      new FileReader(context, batchSize = 7)
    }
  }
}
