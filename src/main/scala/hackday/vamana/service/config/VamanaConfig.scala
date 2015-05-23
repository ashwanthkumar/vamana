package hackday.vamana.service.config

import com.typesafe.config.{Config, ConfigFactory}

case class RequestProcessor(threadPoolSize: Int)

case class VamanaConfig(clusterStoreType: String, requestProcessor: RequestProcessor)

object VamanaConfigReader {
  def load = {
    val config = ConfigFactory.load()
    vamanaConfig(config.getConfig("vamana"))
  }

  private def vamanaConfig(config: Config) = {
    VamanaConfig(
      clusterStoreType = config.getString("cluster-store-impl"),
      requestProcessor = requestProcessor(config.getConfig("request-processor"))
    )
  }

  private def requestProcessor(config: Config) = {
    RequestProcessor(
      threadPoolSize = config.getInt("thread-pool-size")
    )
  }
}
