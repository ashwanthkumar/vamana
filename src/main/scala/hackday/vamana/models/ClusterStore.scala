package hackday.vamana.models

import scala.collection.mutable

trait ClusterStore {
  def save(cluster: Cluster)

  def get(clusterId: Long): Option[Cluster]

  def remove(clusterId: Long)
}


case class InMemoryClusterStore(store: mutable.Map[Long, Cluster]) extends ClusterStore {
  override def save(cluster: Cluster): Unit = store.put(cluster.id, cluster)

  override def get(clusterId: Long) = store.get(clusterId)

  override def remove(clusterId: Long) = store.remove(clusterId)
}

object InMemoryClusterStore {
  private lazy val instance = InMemoryClusterStore(mutable.Map.empty)

  def getInstance = instance
}
