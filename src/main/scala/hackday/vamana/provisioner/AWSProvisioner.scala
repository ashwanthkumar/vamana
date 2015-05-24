package hackday.vamana.provisioner

import java.io.File

import com.google.common.base.Charsets.UTF_8
import com.google.common.base.{Charsets, Predicate}
import com.google.common.io.Files
import hackday.vamana.models._
import hackday.vamana.util.VamanaLogger
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.AWSEC2ProviderMetadata
import org.jclouds.compute.domain._
import org.jclouds.compute.options.{RunScriptOptions, TemplateOptions}
import org.jclouds.compute.{ComputeService, ComputeServiceContext}
import org.jclouds.domain.LoginCredentials
import org.jclouds.io.Payloads
import org.jclouds.logging.LoggingModules
import org.jclouds.sshj.config.SshjSshClientModule

import scala.collection.JavaConverters._
import scala.collection.mutable
import org.jclouds.ec2.compute.EC2ComputeServiceContext
import hackday.vamana.scalar.Scalar


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
      sshClientForNode.apply(node).put(dest, Payloads.newFilePayload(new File(src)))
    }
  }

  def runScriptOn(nodes: List[String], script: String, asUser: String, withPrivateKey: String) = {
    val runScriptOptions = RunScriptOptions.Builder
      .overrideLoginUser(asUser)
      .overrideLoginPrivateKey(withPrivateKey)
      .runAsRoot(false)
    nodes.map(node => {
      node -> computeService.runScriptOnNode(node, script, runScriptOptions)
    }).toMap
  }
}


trait Provisioner {
  def create(cluster: ClusterSpec) : ClusterContext
  def upScale(cluster: ClusterSpec, clusterCtx: ClusterContext, factor: Int) : ClusterContext
  def downScale(cluster: ClusterSpec, clusterCtx: ClusterContext, scalar: Scalar, factor: Int) : ClusterContext
  def tearDown(cluster: ClusterSpec, clusterCtx: ClusterContext)
  def runScriptOn(cluster: ClusterSpec, clusterCtx: ClusterContext, script: String) : Iterable[ExecResponse]
  def bootstrap(cluster: ClusterSpec, clusterCtx: ClusterContext, btstrap: Bootstrap)
  def shutdown: Unit
}

trait LoginDetails {
//  val user = "ec2-user"
  val user = System.getProperty("user.name")
  val privateKeyFile = Option(System.getenv("PRIVATE_KEY_FILE"))
  val privateKey = privateKeyFile.map(pkFile => Files.toString(new File(pkFile), UTF_8))
  val privateKeyWithFallback = privateKey.getOrElse(io.Source.fromFile("/Users/sriram/indix.pem").getLines().mkString("\n"))
}

// FIXME: ComputeServiceContext is a freaking heavy instance. Cache it per <provider, creds>
case class ProviderKey(provider: String, creds: Credentials)
object ClusterProvisioner extends Provisioner with VamanaLogger with LoginDetails {
  val computeServiceCache = mutable.Map[ProviderKey, ComputeService]()

  // NB: Not thread-safe
  def ec2ComputeService(cluster: ClusterSpec) = {
    val hwConfig = cluster.hwConfig
    val creds = hwConfig.credentials
    val providerKey = ProviderKey(ProviderConstants.EC2, creds)
    computeServiceCache.getOrElseUpdate(providerKey, ContextBuilder
      .newBuilder(AWSEC2ProviderMetadata.builder().build())
      .credentials(providerKey.creds.identity, providerKey.creds.credential)
      .modules(Iterable(LoggingModules.firstOrJDKLoggingModule(),new SshjSshClientModule()).asJava)
      .buildView(classOf[ComputeServiceContext]).getComputeService)
  }

  def publicAddressFrom(nodeMetadata: NodeMetadata) = nodeMetadata.getPublicAddresses.asScala.filter(addr => !addr.equals("localhost") && !addr.equals("127.0.0.1")).head
  
  def templateOptions(name: String)(asMaster: Boolean) = {
    val options = new TemplateOptions()
    options.overrideLoginUser(user)
    val optionsWithPrivateKey = privateKey match {
      case Some(key) => options.installPrivateKey(key)
      case _ => options
    }
    val masterOrSlaveTag = if(asMaster) "master" else "slave"
    optionsWithPrivateKey.tags(List(masterOrSlaveTag, name, "vamana").asJava)
  }


  def provisionerFor(cluster: ClusterSpec) = {
    cluster.hwConfig.provider match {
      case ProviderConstants.EC2 => {
        AWSProvisioner(ec2ComputeService(cluster))
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
    LOG.info(s"[INIT] Creating cluster ${cluster.name}")
    val provisioner = provisionerFor(cluster)
    val master = provisioner.addNodes(hwConfig,
        cluster.name,
        1,
        templateFrom(provisioner.computeService)(hwConfig, Some(masterOptions))
      ).head // FIXME: Cause of concern if addNodes fails silently.
    LOG.info(s"Master hostname: ${master.getHostname}")
    LOG.info(s"Master public addr: ${master.getPublicAddresses}")
    LOG.info(s"Master private addr: ${master.getPrivateAddresses}")

    val slaves = provisioner.addNodes(hwConfig,
        cluster.name,
        cluster.appTemplate.minNodes,
        templateFrom(provisioner.computeService)(hwConfig, Some(slaveOptions)))

    LOG.info(s"Slaves running at ..\n${slaves.map(_.getHostname).mkString("\n")}")

    ClusterContext(master, slaves.toSet)
  }

  override def tearDown(cluster: ClusterSpec, clusterCtx: ClusterContext): Unit = {
    LOG.info(s"[SHUTDOWN] Tearing down ${cluster.name}")
    val provisioner = provisionerFor(cluster)
    val nodeIds = clusterCtx.allNodeIds
    val nodeStatus = provisioner.removeNodes(nodeIds)
    LOG.info(s"Cluster ${cluster.name} termination completed.")
    LOG.info(nodeStatus.map(n => s"${n.getHostname} : ${n.getStatus}").mkString("\n"))
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
    LOG.info(s"[UPSCALE] Following new slaves have been added to ${cluster.name}")
    LOG.info(s"${newSlaves.map(_.getPublicAddresses).mkString("[UPSCALE]","\n", "")}")
    clusterCtx.copy(slaves = clusterCtx.slaves ++ newSlaves.toSet)
  }

  override def downScale(cluster: ClusterSpec, clusterCtx: ClusterContext, scalar: Scalar, factor: Int): ClusterContext = {
    val provisioner = provisionerFor(cluster)
    val removedSlaves = provisioner.removeNodes(scalar.downscaleCandidates(factor))
    LOG.info(s"[DOWNSCALE] Following have been removed in ${cluster.name}")
    LOG.info(s"${removedSlaves.map(_.getPublicAddresses).mkString("[DOWNSCALE] ","\n", "")}")
    val removedHosts = removedSlaves.map(_.getHostname)
    clusterCtx.copy(slaves = clusterCtx.slaves.filterNot(s => removedHosts contains s.getHostname))
  }

  override def runScriptOn(cluster: ClusterSpec, clusterCtx: ClusterContext, script: String) = {
    val provisioner = provisionerFor(cluster)
    val results = provisioner.runScriptOn(clusterCtx.allNodeIds, script, user, privateKeyWithFallback)
    LOG.info(s"[RUN_SCRIPT] Executing script $script on ${cluster.name}")
    results.map { case (node, response) =>
      LOG.info(s"Command completed on $node with return code: ${response.getExitStatus}")
      response
    }
  }

  override def bootstrap(cluster: ClusterSpec, clusterCtx: ClusterContext, bootstrapAction: Bootstrap): Unit = {
    LOG.info(s"[BOOTSTRAP] Bootstrapping ${cluster.name}")
    val provisioner = provisionerFor(cluster)
    bootstrapAction.copyActions.foreach{ copy =>
      LOG.info(s"Copying ${copy.src} to ${copy.dst}")
      provisioner.pushFileTo(clusterCtx.all, copy.src, copy.dst)
    }

    bootstrapAction.commands.foreach{ cmd =>
      LOG.info(s"Executing command $cmd")
      provisioner.runScriptOn(clusterCtx.allNodeIds, cmd, user, privateKeyWithFallback)
    }
  }

  override def shutdown = computeServiceCache.values.foreach(_.getContext.close())
}