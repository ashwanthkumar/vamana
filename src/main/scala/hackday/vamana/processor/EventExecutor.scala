package hackday.vamana.processor

import hackday.vamana.models.{Cluster, Event, ClusterStore}
import hackday.vamana.provisioner.{ClusterProvisioner, Provisioner}

class EventExecutor(event: Event, store: ClusterStore) extends Runnable {
  import hackday.vamana.models.Events._
  override def run(): Unit = {
    println(s"I'm executing $event")
    event match {
      case Create(spec) =>
        val uniqueClusterId = store.nextId
        val cluster = Cluster.fromSpec(spec ++ Map("id" -> uniqueClusterId.toString))
        ClusterProvisioner.create(cluster)
    }
  }
}
