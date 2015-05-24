package hackday.vamana.scalar.hadoop

import hackday.vamana.scalar.{Supply, Demand, ResourceStat, Collector}
import hackday.vamana.models.{ClusterStore, RunningCluster}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapred.{TaskReport, TIPStatus, JobClient}
import hackday.vamana.util.{VamanaLogger, Clock}

case class HadoopAppDemand(numJobs: Int, maps: Int, reducers: Int) extends Demand {
  override def quantity: Int = maps + reducers
  def +(other: HadoopAppDemand) = HadoopAppDemand(numJobs + other.numJobs, maps + other.maps, reducers + other.reducers)
}

case class HadoopAppSupply(mapCapacity: Int, reduceCapacity: Int) extends Supply {
  override def available: Int = mapCapacity + reduceCapacity
}

class HadoopMetricsCollector(cluster: RunningCluster, clusterStore: ClusterStore) extends Collector with Clock with VamanaLogger {
  def pendingOrRunning(t: TaskReport) = t.getCurrentStatus == TIPStatus.PENDING || t.getCurrentStatus == TIPStatus.RUNNING

  override def getStats: ResourceStat = {
    val updatedCluster = clusterStore.get(cluster.id)
    updatedCluster.flatMap(_.master).fold(ResourceStat(HadoopAppDemand(0,0,0), HadoopAppSupply(0,0), NOW)){ master =>
      val client = HadoopJobTrackerClient(master)
      val activeJobs = client.runningJobs
      val demand = activeJobs.map { status =>
        val id = status.getJobID
        val mappersDemand = client.mappersInUse(id)
        val reducersDemand = client.reducersInUse(id)
        HadoopAppDemand(1, mappersDemand, reducersDemand)
      }.reduce(_+_)
      val numSlaves = cluster.context.fold(0)(_.slaves.size)
      val supply = HadoopAppSupply(client.maxMapTasks * numSlaves, client.maxReduceTasks * numSlaves)
      ResourceStat(demand, supply, NOW)
    }
  }
}
