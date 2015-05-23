package hackday.vamana.provisioner

import com.google.common.base.Predicate
import hackday.vamana.models.{Cluster, AWSHardwareConfig, HardwareConfig}
import hackday.vamana.util.VamanaLogger
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata
import org.jclouds.compute.options.TemplateOptions
import org.jclouds.compute.{ComputeServiceContext, ComputeService}
import org.jclouds.compute.domain.NodeMetadata

import scala.collection.JavaConverters._


case class AWSProvisioner(computeService: ComputeService) {
  def addNodes(hwConfig: HardwareConfig, clusterName: String, numInstances: Int, templateOptions: Option[TemplateOptions] = None) = {
    val nodes = templateOptions match {
      case Some(opt) => {
        computeService.createNodesInGroup(clusterName, numInstances, opt)
      }
      case _ => computeService.createNodesInGroup(clusterName, numInstances)
    }
    nodes.asScala
  }
  
  def removeNodes(instanceIds: List[String]): Unit = computeService.destroyNodesMatching(new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  })
  
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus
}


trait Provisioner {
  def create(cluster: Cluster)
  def tearDown(cluster: Cluster)
}

trait PrivateKey {
  val privateKey = Option(System.getenv("PRIVATE_KEY_FILE")) getOrElse "/Users/sriram/indix.pem"
}

object ClusterProvisioner extends Provisioner with VamanaLogger with PrivateKey {
  
  def provisionerFor(cluster: Cluster) = {
    cluster.template.hwConfig.provider match {
      case ProviderConstants.EC2 => {
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
    // All nodes that belong to a particular
    // cluster would have the cluster name tag set.
    // This would help querying if need be.
    val options = TemplateOptions.Builder
      .installPrivateKey(privateKey)
      .tags(Iterable(cluster.name).asJava)

    val nodes = provisionerFor(cluster)
      .addNodes(hwConfig, cluster.name, cluster.template.appConfig.minNodes, Some(options))
    LOG.info(s"Provisioned the following nodes: ${nodes.map(_.getHostname).mkString("\n")}")
    LOG.info("TODO: Report an appropriate status!")
  }

  override def tearDown(cluster: Cluster): Unit = {
    LOG.info("Build teardown")
  }
}