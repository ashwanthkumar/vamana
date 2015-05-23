package hackday.vamana.provisioner

import com.google.common.base.Predicate
import hackday.vamana.models.{Cluster, AWSHardwareConfig, HardwareConfig}
import hackday.vamana.util.VamanaLogger
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata
import org.jclouds.compute.options.TemplateOptions
import org.jclouds.compute.{ComputeServiceContext, ComputeService}
import org.jclouds.compute.domain.NodeMetadata

case class AWSProvisioner(computeService: ComputeService) {
  def addNodes(hwConfig: HardwareConfig, numInstances: Int, templateOptions: Option[TemplateOptions] = None) = {
    templateOptions.map(opt => computeService.createNodesInGroup(hwConfig.securityGroup, numInstances, opt))
  }
  
  def removeNodes(instanceIds: List[String]): Unit = computeService.destroyNodesMatching(new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  })
  
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus
}


trait Provisioner {
  def create(cluster: Cluster)
  def teardown(cluster: Cluster)
}

trait PrivateKey {
  val privateKey = ""
}

object ClusterProvisioner extends Provisioner with VamanaLogger with PrivateKey {
  
  def provisionerFor(cluster: Cluster) = {
    cluster.template.hwConfig.provider match {
      case "aws-ec2" => {
        val hwConfig = cluster.template.hwConfig
        val creds = hwConfig.credentials
        val computeService = ContextBuilder
          .newBuilder(AWSEC2ProviderMetadata.builder().build())
          .credentials(creds.identity, creds.credential)
          .buildView(classOf[ComputeServiceContext])
          .getComputeService
        AWSProvisioner(computeService)
      }
      case _ => ???
    }
  }

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
    val nodes = provisionerFor(cluster).addNodes(hwConfig, cluster.template.appConfig.minNodes, Some(TemplateOptions.Builder.installPrivateKey(privateKey)))
    LOG.info(s"Report cluster start status: ${nodes}")
  }

  override def teardown(cluster: Cluster): Unit = {
    LOG.info("Build teardown")
  }
}