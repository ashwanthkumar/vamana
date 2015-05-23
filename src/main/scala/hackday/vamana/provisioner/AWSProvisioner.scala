package hackday.vamana.provisioner

import com.google.common.base.Predicate
import hackday.vamana.models._
import hackday.vamana.util.VamanaLogger
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata
import org.jclouds.compute.domain._
import org.jclouds.compute.options.{RunScriptOptions, TemplateOptions}
import org.jclouds.compute.{ComputeService, ComputeServiceContext}
import org.jclouds.logging.LoggingModules
import org.jclouds.sshj.config.SshjSshClientModule

import scala.collection.JavaConverters._

// TODO: Revisit, if this 'specialization' is needed
case class AWSProvisioner(computeService: ComputeService) {
  def nodeMatcher(instanceIds: List[String]) = new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  }

  def addNodes(hwConfig: HardwareConfig, clusterName: String, numInstances: Int, template: Template) = {
    computeService.createNodesInGroup(clusterName, numInstances, template).asScala
  }

  def removeNodes(instanceIds: List[String]): Set[NodeMetadata] = computeService.suspendNodesMatching(nodeMatcher(instanceIds)).asScala.toSet
  
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus

  def pushFileTo(nodes: List[NodeMetadata], src: String, dest: String) = {
    val sshClientForNode = computeService.getContext.utils().sshForNode()
    nodes.par.foreach{ node =>
      sshClientForNode.apply(node).put(src, dest)
    }
  }

  def runScriptOn(nodes: List[String], script: String, asUser: String, withPrivateKey: String) = {
    val runScriptOptions = RunScriptOptions.Builder
      .overrideLoginUser(asUser)
      .overrideLoginPrivateKey(withPrivateKey)
    nodes.map(node => {
      node -> computeService.runScriptOnNode(node, script, runScriptOptions)
    }).toMap
  }
}


trait Provisioner {
  def create(cluster: ClusterSpec) : ClusterContext
  def upScale(cluster: ClusterSpec, clusterCtx: ClusterContext, factor: Int) : ClusterContext
  def downScale(cluster: ClusterSpec, clusterCtx: ClusterContext, factor: Int) : ClusterContext
  def tearDown(cluster: ClusterSpec, clusterCtx: ClusterContext)
  def runScriptOn(cluster: ClusterSpec, clusterCtx: ClusterContext, script: String) : Iterable[String]
}

trait LoginDetails {
  val user = "ec2-user"
  val privateKeyFile = Option(System.getenv("PRIVATE_KEY_FILE"))
  val privateKey = privateKeyFile.map(pkFile => io.Source.fromFile(pkFile).getLines().mkString("\n"))
  val privateKeyWithFallback = privateKey.getOrElse(io.Source.fromFile("/Users/sriram/indix.pem").getLines().mkString("\n"))
}

// FIXME: ComputeServiceContext is a freaking heavy instance. Cache it per <provider, creds>
object ClusterProvisioner extends Provisioner with VamanaLogger with LoginDetails {

  def templateOptions(name: String)(asMaster: Boolean) = {
    val options = new TemplateOptions()

    val optionsWithPrivateKey = privateKey match {
      case Some(key) => options.installPrivateKey(key)
      case _ => options
    }
    val masterOrSlaveTag = if(asMaster) "master" else "slave"
    optionsWithPrivateKey.clone().tags(List(masterOrSlaveTag, name, "vamana").asJava)
  }


  def provisionerFor(cluster: ClusterSpec) = {
    cluster.hwConfig.provider match {
      case ProviderConstants.EC2 => {
        val hwConfig = cluster.hwConfig
        val creds = hwConfig.credentials
        val computeService = ContextBuilder
          .newBuilder(AWSEC2ProviderMetadata.builder().build())
          .credentials(creds.identity, creds.credential)
          .modules(Iterable(LoggingModules.firstOrJDKLoggingModule(),new SshjSshClientModule()).asJava)
          .buildView(classOf[ComputeServiceContext])
          .getComputeService
        AWSProvisioner(computeService)
      }
      case _ => throw new RuntimeException(s"Only ${ProviderConstants.EC2} supported at this point")
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
  override def create(cluster: ClusterSpec) = {
    val hwConfig = cluster.hwConfig
    val masterOptions = templateOptions(cluster.name)(true)
    val slaveOptions = templateOptions(cluster.name)(false)

    val provisioner = provisionerFor(cluster)
    val master = provisioner.addNodes(hwConfig,
        cluster.name,
        1,
        templateFrom(provisioner.computeService)(hwConfig, Some(masterOptions))
      ).head // FIXME: Cause of concern if addNodes fails silently.
    LOG.info(s"Master bootstrapped and running\n${master.getHostname}")

    val slaves = provisioner.addNodes(hwConfig,
        cluster.name,
        cluster.appTemplate.minNodes,
        templateFrom(provisioner.computeService)(hwConfig, Some(slaveOptions)))

    LOG.info(s"Slaves running at ..\n${slaves.map(_.getHostname).mkString("\n")}")

    ClusterContext(master, slaves.toSet)
  }

  override def tearDown(cluster: ClusterSpec, clusterCtx: ClusterContext): Unit = {
    LOG.info(s"Tearing down ${cluster.name}")
    val provisioner = provisionerFor(cluster)
    val nodeIds = clusterCtx.allNodeIds
    val nodeStatus = provisioner.removeNodes(nodeIds)
    LOG.info("Cluster termination completed...")
    LOG.info(nodeStatus.map(n => s"${n.getHostname} : ${n.getStatus}").mkString("\n"))
    nodeStatus
  }


  override def upScale(cluster: ClusterSpec, clusterCtx: ClusterContext, factor: Int): ClusterContext = {
    val hwConfig = cluster.hwConfig
    val slaveOptions = templateOptions(cluster.name)(false)
    val provisioner = provisionerFor(cluster)
    val newSlaves = provisioner
      .addNodes(hwConfig,
        cluster.name,
        factor,
        templateFrom(provisioner.computeService)(hwConfig, Some(slaveOptions)))
    clusterCtx.copy(slaves = clusterCtx.slaves ++ newSlaves.toSet)
  }

  override def downScale(cluster: ClusterSpec, clusterCtx: ClusterContext, factor: Int): ClusterContext = {
    val provisioner = provisionerFor(cluster)
    val removedSlaves = provisioner.removeNodes(clusterCtx.slaves.take(factor).map(_.getId).toList)
    clusterCtx.copy(slaves = clusterCtx.slaves diff removedSlaves.toSet)
  }

  override def runScriptOn(cluster: ClusterSpec, clusterCtx: ClusterContext, script: String) = {
    val provisioner = provisionerFor(cluster)
    val results = provisioner.runScriptOn(clusterCtx.allNodeIds, script, user, privateKeyWithFallback)
    results.map { case (node, response) =>
      LOG.info(s"Command completed on $node with return code: ${response.getExitStatus}")
      response.getOutput
    }
  }
}