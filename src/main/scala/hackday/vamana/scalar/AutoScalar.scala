package hackday.vamana.scalar

import hackday.vamana.models.{Running, ClusterStore}
import hackday.vamana.models.Events.{DoNothing, Downscale, Upscale}
import hackday.vamana.processor.RequestProcessor
import hackday.vamana.util.VamanaLogger

case class AutoScaleConfig(hustlePeriod: Long, upscaleBurstRate: Double, downscaleBurstRate: Double)

object AutoScaleConfig {
  def apply(): AutoScaleConfig = AutoScaleConfig(1000, 100.0, 25.0)
}

class AutoScalar(appScalar: Scalar, config: AutoScaleConfig, metricsStore: MetricStore, clusterId: Long, clusterStore: ClusterStore) extends Runnable with VamanaLogger {
  var lastCheckTime: Long = 0

  override def run(): Unit = {
    if (lastCheckTime < 1) {
      lastCheckTime = System.currentTimeMillis()
    } else if (System.currentTimeMillis() - lastCheckTime >= config.hustlePeriod) {
      val stats = metricsStore.get(clusterId)
      stats match {
        case latest :: rest =>
          val scaleUnit = appScalar.scaleUnit(latest)
          // TODO - Need to take clusterContext from clusterStore and check if we've already reached the limit on the number of nodes
          for (
            cluster <- clusterStore.get(clusterId)
            if cluster.isNotInFullCapacity
            if cluster.runningNodes < cluster.maxNodes
            if cluster.status == Running
          ) yield RequestProcessor.processEvent(createScaleEvent(scaleUnit.numberOfNodes, cluster.maxNodes, cluster.minNodes, cluster.runningNodes))

        case Nil =>
          LOG.warn(s"$clusterId has no metrics collected yet")
      }
    } else {
      // wait until hustlePeriod
    }
  }

  def createScaleEvent(autoScaleRequest: Int, maxNodesInCluster: Int, minNodesInCluster: Int, runningNodes: Int) = {
    val upscalePool = maxNodesInCluster - runningNodes
    val downscalePool = runningNodes - minNodesInCluster
    autoScaleRequest match {
      case upscaleRequest if upscaleRequest > 0 && upscalePool > 0 => Upscale(clusterId, math.min(upscalePool, upscaleRequest))
      case numberOfNodes if autoScaleRequest < 0 &&  downscalePool > 0 => Downscale(clusterId, math.min(downscalePool, math.abs(autoScaleRequest)))
      case _ => DoNothing
    }
  }
}
