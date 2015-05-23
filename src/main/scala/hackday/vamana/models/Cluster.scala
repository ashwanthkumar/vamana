package hackday.vamana.models

import hackday.vamana.provisioner.ProviderConstants

case class Credentials(identity: String, credential: String)

trait HardwareConfig {
  def provider: String

  def credentials: Credentials
}

object HardwareConfig {
  def fromSpec(spec: Map[String, String]) = {
    spec("cloud") match {
      case "aws" =>
        val config = AWSHardwareConfig(
          accessKeyId = spec("accessId"),
          secretKeyId = spec("secretKey"),
          ami = spec.getOrElse("ami", "foo-bar-ami"),
          instanceType = spec.getOrElse("machineType", "t1.micro")
        )

        spec.get("spot-price").map(price => config.copy(spotPrice = Some(price.toDouble))).getOrElse(config)
      case cloud => throw new RuntimeException(s"We still don't support $cloud cloud provider")
    }
  }
}

trait AppTemplate {
  def maxNodes: Int

  def minNodes: Int
}


object AppTemplate {
  def fromSpec(spec: Map[String, String]) = {
    spec("app") match {
      case "hadoop" =>
        HadoopTemplate(
          props = spec,
          minNodes = spec.getOrElse("minNodes", "1").toInt,
          maxNodes = spec.getOrElse("maxNodes", "2").toInt
        )
      case app => throw new RuntimeException(s"We still don't know an application for $app")
    }
  }
}

case class AppContext(master: String, slaves: List[String])

case class HadoopTemplate(props: Map[String, String], minNodes: Int, maxNodes: Int) extends AppTemplate

case class AWSHardwareConfig(accessKeyId: String, secretKeyId: String,
                             ami: String, instanceType: String,
                             spotPrice: Option[Double] = None) extends HardwareConfig {
  override def provider: String = ProviderConstants.EC2

  override def credentials: Credentials = Credentials(accessKeyId, secretKeyId)
}

case class ClusterTemplate(appConfig: AppTemplate, hwConfig: HardwareConfig)

object ClusterTemplate {
  def fromSpec(spec: Map[String, String]) = {
    ClusterTemplate(
      appConfig = AppTemplate.fromSpec(spec),
      hwConfig = HardwareConfig.fromSpec(spec)
    )
  }
}

case class Cluster(id: Long, name: String, template: ClusterTemplate, status: ClusterStatus)

object Cluster {
  def fromSpec(spec: Map[String, String]) = {
    Cluster(
      id = spec("id").toLong,
      name = spec("name"),
      template = ClusterTemplate.fromSpec(spec),
      status = NotRunning
    )
  }
}
