package hackday.vamana.processor

import hackday.vamana.models.{Cluster, Event, ClusterStore}
import hackday.vamana.provisioner.Provisioner

class EventExecutor(event: Event, store: ClusterStore, provisioner: Provisioner) extends Runnable {
  import hackday.vamana.models.Events._
  override def run(): Unit = {
    println(s"I'm executing $event")
    event match {
      case Create(spec) =>
        val uniqueClusterId = store.nextId
        provisioner.create(Cluster.fromSpec(spec ++ Map("id" -> uniqueClusterId.toString)))
    }
  }
}
