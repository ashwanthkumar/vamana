package hackday.vamana.service.config

import com.typesafe.config.{Config, ConfigFactory}

case class AWSCred(accessId: String, secretKey: String)
case class RequestProcessor(threadPoolSize: Int)

case class VamanaConfig(clusterStoreType: String, aws: AWSCred, requestProcessor: RequestProcessor)

object VamanaConfigReader {
  def load = {
    val config = ConfigFactory.load()
    vamanaConfig(config.getConfig("vamana"))
  }

  private def vamanaConfig(config: Config) = {
    VamanaConfig(
      clusterStoreType = config.getString("cluster-store-impl"),
      aws = awsConfig(config.getConfig("ec2")),
      requestProcessor = requestProcessor(config.getConfig("request-processor"))
    )
  }

  private def awsConfig(config: Config) = {
    AWSCred(
      accessId = config.getString("access-id"),
      secretKey = config.getString("secret-key")
    )
  }

  private def requestProcessor(config: Config) = {
    RequestProcessor(
      threadPoolSize = config.getInt("thread-pool-size")
    )
  }
}
