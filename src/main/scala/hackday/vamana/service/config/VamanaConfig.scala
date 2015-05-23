package hackday.vamana.service.config

import com.typesafe.config.{Config, ConfigFactory}

case class VamanaConfig(clusterStoreType: String)

object VamanaConfigReader {
  def load = {
    val config = ConfigFactory.load()
    vamanaConfig(config.getConfig("vamana"))
  }

  private def vamanaConfig(config: Config) = {
    VamanaConfig(
      clusterStoreType = config.getString("cluster-store-impl")
    )
  }
}
