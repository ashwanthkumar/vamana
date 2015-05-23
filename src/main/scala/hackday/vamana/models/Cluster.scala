package hackday.vamana.models

import hackday.vamana.provisioner.ProviderConstants
import org.jclouds.compute.domain.NodeMetadata


case class ClusterContext(master: NodeMetadata, slaves: Set[NodeMetadata])

case class HadoopTemplate(props: Map[String, String], minNodes: Int, maxNodes: Int) extends AppTemplate

case class AWSHardwareConfig(accessKeyId: String, secretKeyId: String,
                             ami: String, instanceType: String,
                             spotPrice: Option[Double] = None) extends HardwareConfig {
  override def provider: String = ProviderConstants.EC2

  override def credentials: Credentials = Credentials(accessKeyId, secretKeyId)
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
