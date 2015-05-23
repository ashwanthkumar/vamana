package hackday.vamana.models

import hackday.vamana.provisioner.ProviderConstants
import org.jclouds.compute.domain.NodeMetadata
import scala.collection.JavaConverters._

case class ClusterContext(master: NodeMetadata, slaves: Set[NodeMetadata]) {
  def allNodeIds = all.map(_.getId)
  def all = master :: slaves.toList
}

case class HadoopTemplate(props: Map[String, String], minNodes: Int, maxNodes: Int) extends AppTemplate {
  override def context(clusterCtx: ClusterContext): AppContext = AppContext(null, null, HadoopLifeCycle(clusterCtx, props))
}

case class AWSHardwareConfig(accessKeyId: String, secretKeyId: String,
                             instanceType: String,
                             spotPrice: Option[Double] = None) extends HardwareConfig {
  // TODO: FILL THIS MAPPING
  val machineTypeToAmiMapping = Map[String, String](
    "t2.small" -> "us-west-2/ami-5189a661"
  )
  val defaultAmiId = "us-east-1/ami-de57dcb6" // wont be available unless you use ix IAM creds

  override def provider: String = ProviderConstants.EC2
  override def imageId: String = machineTypeToAmiMapping.getOrElse(instanceType, defaultAmiId)
  override def credentials: Credentials = Credentials(accessKeyId, secretKeyId)
}

case class RunningCluster(id: Long, spec: ClusterSpec, status: ClusterStatus, context: Option[ClusterContext] = None) {
  def master = context.map(_.master).map(_.getPublicAddresses.asScala.filter(_ != "localhost").head)
}
