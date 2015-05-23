package hackday.vamana.scalar.myservice

import hackday.vamana.scalar.{ResourceStat, Collector}
import hackday.vamana.models.RunningCluster

class MyServiceCollector(cluster: RunningCluster) extends Collector {
  override def getStats: ResourceStat = ???
}
