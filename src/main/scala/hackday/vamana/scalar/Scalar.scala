package hackday.vamana.scalar

import hackday.vamana.models.RunningCluster

import scala.collection.mutable

trait MetricStore {
  def put(clusterId: Long, stat: ResourceStat)

  def get(clusterId: Long): List[ResourceStat]
}
object MetricStore {
  def apply(storeType: String) = storeType match {
    case "memory" => InMemoryMetricStore.getInstance
    case store => throw new RuntimeException(s"Unknown type of MetricStore -  $store")
  }
}

case class InMemoryMetricStore(store: mutable.Map[Long, List[ResourceStat]]) extends MetricStore {
  override def put(clusterId: Long, stat: ResourceStat): Unit = {
    val stats = store.getOrElse(clusterId, List())
    store.put(clusterId, stat :: stats)
  }

  override def get(clusterId: Long): List[ResourceStat] = store.getOrElse(clusterId, List())
}
object InMemoryMetricStore {
  private val instance = InMemoryMetricStore(mutable.Map.empty)

  def getInstance = instance
}

case class ScaleUnit(numberOfNodes: Int)

abstract class Scalar(cluster: RunningCluster) {
  def scaleUnit(normalizedStat: ResourceStat): ScaleUnit

  def downscaleCandidates(number: Int): List[String]
}
