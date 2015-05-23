package hackday.vamana.models

trait HardwareConfig { def provider: String}

trait AppConfig {}

case class HadoopConfig(slaves: List[String], master: String, props: Map[String, String],
                        minNodes: Int, maxNodes: Int) extends AppConfig

case class AWSHardwareConfig(accessKeyId: String, secretKeyId: String,
                             ami: String, instanceType: String,
                             securityGroup: String, spotPrice: Option[Double] = None) extends HardwareConfig {
  override def provider: String = "ec2"
}

case class ClusterTemplate(appConfig: AppConfig, hwConfig: HardwareConfig)

case class Cluster(id: Long, name: String, template: ClusterTemplate, status: ClusterStatus)
