package hackday.vamana.models

import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable

trait ClusterStore {
  def nextId: Long

  def save(cluster: RunningCluster)

  def get(clusterId: Long): Option[RunningCluster]

  def remove(clusterId: Long)
}

object ClusterStore {
  def apply(storeType: String) = storeType match {
    case "memory" => InMemoryClusterStore.getInstance
    case x => throw new RuntimeException(s"We still don't have support for $x")
  }
}


case class InMemoryClusterStore(store: mutable.Map[Long, RunningCluster]) extends ClusterStore {
  private val idService = new AtomicLong(1l)

  override def save(cluster: RunningCluster): Unit = store.put(cluster.id, cluster)

  override def get(clusterId: Long) = store.get(clusterId)

  override def remove(clusterId: Long) = store.remove(clusterId)

  override def nextId: Long = idService.getAndIncrement
}

object InMemoryClusterStore {
  private lazy val instance = InMemoryClusterStore(mutable.Map.empty)

  def getInstance = instance
}
