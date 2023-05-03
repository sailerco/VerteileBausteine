import com.typesafe.config.{Config, ConfigFactory}

object Utils {
  def startup(port: Int): Config = ConfigFactory
    .parseString(s"akka.remote.artery.canonical.port=$port")
    .withFallback(ConfigFactory.load())
}
