import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors


object Guard {
  def apply(): Behavior[Nothing] = {
    Behaviors.setup[Receptionist.Listing] { context =>
      StoreShard.initSharding(context.system)
      if (context.system.settings.config.getInt("akka.remote.artery.canonical.port") == 25251) {
        context.spawnAnonymous(Store(context.system))
      }

      if (context.system.settings.config.getInt("akka.remote.artery.canonical.port") == 25252)
        context.system.receptionist ! Receptionist.Subscribe(Store.StoreService, context.self)

      if (context.system.settings.config.getInt("akka.remote.artery.canonical.port") == 25253)
        context.system.receptionist ! Receptionist.Subscribe(Client.ClientService, context.self)


      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case Store.StoreService.Listing(listings) =>
          listings.foreach { ps =>
            new Server(context.system, ps)
            context.spawnAnonymous(Client(ps))
          }
          Behaviors.same
        case Client.ClientService.Listing(listings) =>
          listings.foreach { ps =>
            //new Server(ps)
            /*ps ! Client.Set("IT", "Italia")
            ps ! Client.Get("IT")
            ps ! Client.Get("DE")
            val reader = context.spawnAnonymous(FileReader())
            val filename = "trip_data.csv"
            reader ! FileReader.File(filename, ps)*/
          }
          Behaviors.same
      }
    }.narrow
  }
}