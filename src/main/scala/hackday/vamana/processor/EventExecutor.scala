package hackday.vamana.processor

import hackday.vamana.models._
import hackday.vamana.provisioner.ClusterProvisioner
import hackday.vamana.util.VamanaLogger

class EventExecutor(event: Event, store: ClusterStore) extends Runnable with VamanaLogger {
  import hackday.vamana.models.Events._
  override def run(): Unit = {
    println(s"I'm executing $event")
    event match {
      case Create(spec) =>
        val uniqueClusterId = store.nextId
        val clusterSpec = ClusterSpec.fromSpec(spec ++ Map("id" -> uniqueClusterId.toString))
        val clusterContext = ClusterProvisioner.create(clusterSpec)
        val runningCluster = RunningCluster(uniqueClusterId, clusterSpec, Running, Some(clusterContext))
        LOG.info(s"Started cluster ${clusterSpec.name}(${runningCluster.id})")
        LOG.info(s"Master - ${clusterContext.master.getPublicAddresses} / ${clusterContext.master.getPrivateAddresses}")
        LOG.info("Slaves - ")
        clusterContext.slaves.foreach(slave => LOG.info(s"Started slave - ${slave.getPublicAddresses} / ${slave.getPrivateAddresses}"))
        store.save(runningCluster)
    }
  }
}
