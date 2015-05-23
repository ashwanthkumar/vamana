package hackday.vamana.provisioner

import com.google.common.base.Predicate
import hackday.vamana.models.{ClusterContext, Cluster, HardwareConfig}
import hackday.vamana.util.VamanaLogger
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata
import org.jclouds.compute.options.TemplateOptions
import org.jclouds.compute.{domain, ComputeServiceContext, ComputeService}
import org.jclouds.compute.domain._

import scala.collection.JavaConverters._
import org.jclouds.logging.LoggingModules
import org.jclouds.sshj.config.SshjSshClientModule
import org.jclouds.ec2.compute.{EC2ComputeServiceContext, EC2ComputeService}
import hackday.vamana.models.ClusterContext
import scala.Some


case class AWSProvisioner(computeService: ComputeService) {
  def addNodes(hwConfig: HardwareConfig, clusterName: String, numInstances: Int, template: Template) = {
//    val ec2ComputeSvcContext = computeService.getContext.unwrapApi(classOf[EC2ComputeServiceContext])
    computeService.createNodesInGroup(clusterName, numInstances, template).asScala
  }

  def removeNodes(instanceIds: List[String]): Unit = computeService.destroyNodesMatching(new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  })
  
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus
}


trait Provisioner {
  def create(cluster: Cluster) : ClusterContext
  def tearDown(cluster: Cluster)
}

trait PrivateKey {
  val privateKeyFile = Option(System.getenv("PRIVATE_KEY_FILE")) getOrElse "/Users/sriram/indix.pem"
  val privateKey = io.Source.fromFile(privateKeyFile).getLines().mkString("\n")
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
          .modules(Iterable(LoggingModules.firstOrJDKLoggingModule(),new SshjSshClientModule()).asJava)
          .buildView(classOf[ComputeServiceContext])
          .getComputeService
        AWSProvisioner(computeService)
      }
      case _ => ???
    }
  }

  // TODO: Move it inside HardwareConfig
  def templateFrom(computeService: ComputeService)(hwConfig: HardwareConfig, templateOptions: Option[TemplateOptions]): Template = {
    val templateBuilder = computeService.templateBuilder().hardwareId(hwConfig.instanceType).imageId(hwConfig.imageId)
    val template = templateOptions.fold(templateBuilder)(opt => templateBuilder.options(opt)).build()
    template
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
    // cluster would have the cluster name as tag.
    // This would help querying if need be.
    val tags = List(cluster.name)
    val options = TemplateOptions.Builder
      .installPrivateKey(privateKey)
      .tags(tags.asJava)

    val masterOptions = options.clone().tags(("master" :: tags).asJava)
    val slaveOptions = options.clone().tags(("slave" :: tags).asJava)

    val provisioner = provisionerFor(cluster)
    val master = provisionerFor(cluster)
      .addNodes(hwConfig,
        cluster.name,
        1,
        templateFrom(provisioner.computeService)(hwConfig, Some(masterOptions))
      ).head
    LOG.info(s"Master running at => ${master.getHostname}")

    val slaves = provisionerFor(cluster)
      .addNodes(hwConfig,
        cluster.name,
        cluster.template.appConfig.minNodes,
        templateFrom(provisioner.computeService)(hwConfig, Some(slaveOptions)))

    LOG.info(s"Slaves running at => ${slaves.map(_.getHostname).mkString("\n")}")

    ClusterContext(master, slaves.toSet)
  }

  override def tearDown(cluster: Cluster): Unit = {

    LOG.info("Build teardown")
  }
}