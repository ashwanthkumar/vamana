package hackday.vamana.processor

import hackday.vamana.models._
import hackday.vamana.provisioner.ClusterProvisioner
import hackday.vamana.util.VamanaLogger

class EventExecutor(event: Event, store: ClusterStore) extends Runnable with VamanaLogger {

  import hackday.vamana.models.Events._

  override def run(): Unit = {
    event match {
      case Create(spec) =>
        val uniqueClusterId = store.nextId
        LOG.info(s"Starting to create cluster with ")
        val clusterSpec = ClusterSpec.fromSpec(spec ++ Map("id" -> uniqueClusterId.toString))
        val clusterContext = ClusterProvisioner.create(clusterSpec)
        val runningCluster = RunningCluster(uniqueClusterId, clusterSpec, Running, Some(clusterContext))
        LOG.info(s"Started cluster ${clusterSpec.name}(${runningCluster.id})")
        LOG.info(s"Master - ${clusterContext.master.getPublicAddresses} / ${clusterContext.master.getPrivateAddresses}")
        LOG.info("Slaves - ")
        clusterContext.slaves.foreach(slave => LOG.info(s"Started slave - ${slave.getPublicAddresses} / ${slave.getPrivateAddresses}"))
        store.save(runningCluster)

      case Upscale(clusterId, factor) =>
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          ClusterProvisioner.upScale(cluster.spec, context, factor)
          LOG.info(s"Upscaled the ${cluster.spec.name} by $factor nodes")
        }

      case Downscale(clusterId, factor) =>
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          ClusterProvisioner.downScale(cluster.spec, context, factor)
          LOG.info(s"Downscaled the ${cluster.spec.name} by $factor nodes")
        }
    }
  }
}
