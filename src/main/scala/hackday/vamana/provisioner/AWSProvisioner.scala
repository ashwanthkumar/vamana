package hackday.vamana.provisioner

import com.google.common.base.Predicate
import hackday.vamana.models.{Cluster, AWSHardwareConfig, HardwareConfig}
import org.jclouds.ContextBuilder
import org.jclouds.compute.ComputeService
import org.jclouds.compute.domain.NodeMetadata

case class AWSProvisioner(computeService: ComputeService) {
  def addNodes(hwConfig: AWSHardwareConfig, numInstances: Int) = computeService.createNodesInGroup(hwConfig.securityGroup, numInstances)
  def removeNodes(instanceIds: List[String]): Unit = computeService.destroyNodesMatching(new Predicate[NodeMetadata] {
    override def apply(t: NodeMetadata): Boolean = instanceIds contains t.getId
  })
  def status(instanceId: String): Unit = computeService.getNodeMetadata(instanceId).getStatus
}


class ClusterProvisioner(cluster: Cluster) {

}