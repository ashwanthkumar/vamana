package hackday.vamana.processor

import java.util.concurrent.Executors

import hackday.vamana.models.{ClusterStore, Event}
import hackday.vamana.service.config.VamanaConfigReader

object RequestProcessor {
  private val config = VamanaConfigReader.load
  private val clusterStore = ClusterStore(config.clusterStoreType)
  private val executor = Executors.newCachedThreadPool()

  def process(event: Event) = executor.execute(new EventExecutor(event, clusterStore))
}
