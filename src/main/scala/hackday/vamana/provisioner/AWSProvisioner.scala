package hackday.vamana.provisioner

import com.google.common.base.Predicate
import hackday.vamana.models.{Cluster, AWSHardwareConfig, HardwareConfig}
import hackday.vamana.util.VamanaLogger
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata
import org.jclouds.compute.{ComputeServiceContext, ComputeService}
import org.jclouds.compute.domain.NodeMetadata

case class AWSProvisioner(computeService: ComputeService) {
  def addNodes(hwConfig: HardwareConfig, numInstances: Int) = {
    computeService.createNodesInGroup(hwConfig.securityGroup, numInstances)
  }
  def removeNodes(instanceIds: List[String]): Unit = computeService.destroyNodesMatching(new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  })
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus
}


trait Provisioner {
  def provisionerFor(provider: String)(computeService: ComputeService) = {
    provider match {
      case "aws-ec2" => AWSProvisioner(computeService)
      case _ => ???
    }
  }
  def create(cluster: Cluster)
  def teardown(cluster: Cluster)
}


object ClusterProvisioner extends Provisioner with VamanaLogger {
  /*
  * Expected to provision a cluster
  *
  * - Uses ContextBuilder to build the computeServiceContext
  * - Get the provider specific provisioner
  * - delegate calls
  * */
  // TODO: Override properties with appropriate ami query
  override def create(cluster: Cluster) = {
    val hwConfig = cluster.template.hwConfig
    val creds = hwConfig.credentials
    val computeService = ContextBuilder
      .newBuilder(AWSEC2ProviderMetadata.builder().build())
      .credentials(creds.identity, creds.credential)
      .buildView(classOf[ComputeServiceContext])
      .getComputeService
    val nodes = provisionerFor(hwConfig.provider)(computeService).addNodes(hwConfig, cluster.template.appConfig.minNodes)
    LOG.info(s"Report cluster start status: ${nodes}")
  }

  override def teardown(cluster: Cluster): Unit = {
    val hwConfig = cluster.template.hwConfig
    val creds = hwConfig.credentials
    val computeService = ContextBuilder
      .newBuilder(AWSEC2ProviderMetadata.builder().build())
      .credentials(creds.identity, creds.credential)
      .buildView(classOf[ComputeServiceContext])
      .getComputeService
    LOG.info("Build teardown")
  }
}