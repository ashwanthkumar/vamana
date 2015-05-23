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
    computeService.createNodesInGroup(clusterName, numInstances, template).asScala
  }

  def removeNodes(instanceIds: List[String]): Set[NodeMetadata] = computeService.destroyNodesMatching(new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  }).asScala.toSet
  
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus
}


trait Provisioner {
  def create(cluster: Cluster) : ClusterContext
  def tearDown(cluster: Cluster, clusterCtx: ClusterContext)
}

trait PrivateKey {
  val privateKeyFile = Option(System.getenv("PRIVATE_KEY_FILE"))
  val privateKey = privateKeyFile.map(pkFile => io.Source.fromFile(pkFile).getLines().mkString("\n"))
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
      .tags(tags.asJava)

    val optionsWithPrivateKey = privateKey match {
      case Some(key) => options.installPrivateKey(key)
      case _ => options
    }

    val masterOptions = optionsWithPrivateKey.clone().tags(("master" :: tags).asJava)
    val slaveOptions = optionsWithPrivateKey.clone().tags(("slave" :: tags).asJava)

    val provisioner = provisionerFor(cluster)
    val master = provisioner
      .addNodes(hwConfig,
        cluster.name,
        1,
        templateFrom(provisioner.computeService)(hwConfig, Some(masterOptions))
      ).head
    LOG.info(s"Master running at => ${master.getHostname}")

    val slaves = provisioner
      .addNodes(hwConfig,
        cluster.name,
        cluster.template.appConfig.minNodes,
        templateFrom(provisioner.computeService)(hwConfig, Some(slaveOptions)))

    LOG.info(s"Slaves running at => ${slaves.map(_.getHostname).mkString("\n")}")

    ClusterContext(master, slaves.toSet)
  }

  override def tearDown(cluster: Cluster, clusterCtx: ClusterContext): Unit = {
    LOG.info(s"Tearing down ${cluster.name}")

    val provisioner = provisionerFor(cluster)
    val nodeIds = clusterCtx.master.getId :: clusterCtx.slaves.toList.map(_.getId)
    val nodeStatus = provisioner.removeNodes(nodeIds)
    println("Cluster termination completed...")
    println(nodeStatus.map(n => s"${n.getHostname} : ${n.getStatus}").mkString("\n"))
    nodeStatus
  }
}