package hackday.vamana.scalar

import hackday.vamana.models.RunningCluster

import scala.collection.mutable

trait MetricStore {
  def put(clusterId: Long, stat: ResourceStat)

  def get(clusterId: Long): List[ResourceStat]
}

case class InMemoryMetricStore(store: mutable.Map[Long, List[ResourceStat]]) extends MetricStore {
  override def put(clusterId: Long, stat: ResourceStat): Unit = {
    val stats = store.getOrElse(clusterId, List())
    store.put(clusterId, stat :: stats)
  }

  override def get(clusterId: Long): List[ResourceStat] = store.getOrElse(clusterId, List())
}

case class ScaleUnit(numberOfNodes: Int)

abstract class Scalar(cluster: RunningCluster) {
  def scaleUnit(normalizedStat: ResourceStat): ScaleUnit

  def downscaleCandidates(number: Int): List[String]
}
