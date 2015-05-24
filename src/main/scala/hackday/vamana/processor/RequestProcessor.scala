package hackday.vamana.processor

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import hackday.vamana.models.{ClusterStore, Event, RunningCluster}
import hackday.vamana.scalar._
import hackday.vamana.service.config.VamanaConfigReader
import hackday.vamana.util.VamanaLogger

import scala.collection.mutable

object RequestProcessor {
  private val config = VamanaConfigReader.load
  private val clusterStore = ClusterStore(config.clusterStoreType)
  private val eventExecutor = Executors.newCachedThreadPool()

  private val collectorExecutor = Executors.newScheduledThreadPool(4)
  private val metricsStore = MetricStore(config.metricsStoreType)
  private val collectorsInProgress = mutable.Map[Long, ScheduledFuture[_]]()

  private val autoScalar = Executors.newScheduledThreadPool(4)
  private val autoScalarsInProgress = mutable.Map[Long, ScheduledFuture[_]]()


  def processEvent(event: Event) = eventExecutor.execute(new EventExecutor(event, clusterStore))

  def startCollector(cluster: RunningCluster, collector: Collector) = {
    val future = collectorExecutor.scheduleAtFixedRate(new CollectorExecutor(cluster.id, collector, metricsStore), 1, 30, TimeUnit.SECONDS)
    collectorsInProgress.put(cluster.id, future)
  }

  def stopCollector(clusterId: Long) = collectorsInProgress.get(clusterId) foreach (f => f.cancel(false))

  def startAutoScalar(clusterId: Long, appScalar: Scalar) = {
    val config = AutoScaleConfig() // FIXME - this should ideally come from clusterSpec
    val future = autoScalar.scheduleAtFixedRate(new AutoScalar(appScalar, config, metricsStore, clusterId), config.hustlePeriod, config.hustlePeriod, TimeUnit.MILLISECONDS)
    autoScalarsInProgress.put(clusterId, future)
  }

  def stopAutoScalar(clusterId: Long) = autoScalarsInProgress.get(clusterId) foreach (f => f.cancel(false))
}

class CollectorExecutor(clusterId: Long, collector: Collector, store: MetricStore) extends Runnable with VamanaLogger {

  override def run(): Unit = {
    try {
      val stat = collector.getStats
      store.put(clusterId, stat)
    } catch {
      case e: Exception =>
        LOG.error(e.getMessage, e)
    }
  }
}
