package hackday.vamana.processor

import hackday.vamana.models.{Cluster, Event, ClusterStore}
import hackday.vamana.provisioner.{ClusterProvisioner, Provisioner}
import hackday.vamana.util.VamanaLogger

class EventExecutor(event: Event, store: ClusterStore) extends Runnable with VamanaLogger {
  import hackday.vamana.models.Events._
  override def run(): Unit = {
    println(s"I'm executing $event")
    event match {
      case Create(spec) =>
        val uniqueClusterId = store.nextId
        val cluster = Cluster.fromSpec(spec ++ Map("id" -> uniqueClusterId.toString))
        val clusterContext = ClusterProvisioner.create(cluster)
        LOG.info(s"Started cluster ${cluster.name}(${cluster.id})")
        LOG.info(s"Master - ${clusterContext.master.getPublicAddresses} / ${clusterContext.master.getPrivateAddresses}")
        LOG.info("Slaves - ")
        clusterContext.slaves.foreach(slave => LOG.info(s"Started slave - ${slave.getPublicAddresses} / ${slave.getPrivateAddresses}"))
    }
  }
}
