package hackday.vamana.models

import hackday.vamana.provisioner.ProviderConstants
import org.jclouds.compute.domain.NodeMetadata
import scala.collection.JavaConverters._
import hackday.vamana.scalar.myservice.{MyServiceCollector, MyServiceScalar}
import hackday.vamana.scalar.hadoop.{HadoopScalar, HadoopMetricsCollector}

case class ClusterContext(master: NodeMetadata, slaves: Set[NodeMetadata]) {
  def allNodeIds = all.map(_.getId)
  def all = master :: slaves.toList
}

case class HadoopTemplate(props: Map[String, String], minNodes: Int, maxNodes: Int) extends AppTemplate {
  override def context(cluster: RunningCluster, clusterStore: ClusterStore): AppContext = AppContext(
    new HadoopMetricsCollector(cluster, clusterStore),
    new HadoopScalar(cluster),
    HadoopLifeCycle(cluster.context.get, props)
  )
}

case class MyServiceTemplate(props: Map[String, String], minNodes: Int, maxNodes: Int) extends AppTemplate {
  override def context(cluster: RunningCluster, clusterStore: ClusterStore): AppContext = AppContext(
    new MyServiceCollector(cluster, clusterStore),
    new MyServiceScalar(cluster),
    MyServiceLifeCycle(cluster.context.get)
  )
}

case class AWSHardwareConfig(accessKeyId: String, secretKeyId: String,
                             instanceType: String,
                             spotPrice: Option[Double] = None) extends HardwareConfig {
  // TODO: FILL THIS MAPPING
  val machineTypeToAmiMapping = Map[String, String](
    "t2.small" -> "us-east-1/ami-de57dcb6"
  )
  val defaultAmiId = "us-east-1/ami-de57dcb6" // wont be available unless you use ix IAM creds

  override def provider: String = ProviderConstants.EC2
  override def imageId: String = machineTypeToAmiMapping.getOrElse(instanceType, defaultAmiId)
  override def credentials: Credentials = Credentials(accessKeyId, secretKeyId)
}

case class RunningCluster(id: Long, spec: ClusterSpec, status: ClusterStatus, context: Option[ClusterContext] = None) {
  def master = context.map(_.master).map(_.getPublicAddresses.asScala.filter(_ != "localhost").head)

  def isInFullCapacity = context.exists(ctx => spec.appTemplate.maxNodes == ctx.slaves.size)
  def isNotInFullCapacity = !isInFullCapacity

  def runningNodes = context.map(ctx => ctx.slaves.size).getOrElse(0)
  def maxNodes = spec.appTemplate.maxNodes
  def minNodes = spec.appTemplate.minNodes

  def addNodes(nodes: Set[_ <: NodeMetadata]) = {
    val newContext = context.map(ctx => ctx.copy(slaves = ctx.slaves ++ nodes))
    this.copy(context = newContext)
  }
}
