package hackday.vamana.models

case class Credentials(identity: String, credential: String)

trait HardwareConfig {
  def securityGroup: String
  def provider: String
  def credentials: Credentials
}

trait AppConfig {
  def maxNodes : Int
  def minNodes : Int
  def master: String
  def slaves: List[String]
}

case class HadoopConfig(slaves: List[String], master: String, props: Map[String, String],
                        minNodes: Int, maxNodes: Int) extends AppConfig

case class AWSHardwareConfig(accessKeyId: String, secretKeyId: String,
                             ami: String, instanceType: String,
                             securityGroup: String, spotPrice: Option[Double] = None) extends HardwareConfig {
  override def provider: String = "ec2"

  override def credentials: Credentials = Credentials(accessKeyId, secretKeyId)
}

case class ClusterTemplate(appConfig: AppConfig, hwConfig: HardwareConfig)

case class Cluster(id: Long, name: String, template: ClusterTemplate, status: ClusterStatus)
