package hackday.vamana.scalar.hadoop

import hackday.vamana.scalar.{Supply, Demand, ResourceStat, Collector}
import hackday.vamana.models.RunningCluster
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

class HadoopMetricsCollector(cluster: RunningCluster) extends Collector with Clock with VamanaLogger {
  def pendingOrRunning(t: TaskReport) = t.getCurrentStatus == TIPStatus.PENDING || t.getCurrentStatus == TIPStatus.RUNNING
  override def getStats: ResourceStat = {
    cluster.master.fold(ResourceStat(HadoopAppDemand(0,0,0), HadoopAppSupply(0,0), NOW)){ master =>
      val conf = new Configuration
      conf.set("mapred.job.tracker", s"$master:8021")
      val client = new JobClient(conf)
      val status = client.getClusterStatus(true)
      println("active TTs = " + status.getTaskTrackers)
      println("blacklisted TTs = " + status.getBlacklistedTrackers)
      println("active map tasks = " + status.getMapTasks)
      println("total map tasks = " + status.getMaxMapTasks)
      println("active reduce tasks = " + status.getReduceTasks)
      println("total reduce tasks = " + status.getMaxReduceTasks)
      val activeJobs = client.jobsToComplete()
      val demand = activeJobs.map { status =>
        val id = status.getJobID
        val mappersDemand = client.getMapTaskReports(id).count(pendingOrRunning)
        val reducersDemand = client.getReduceTaskReports(id).count(pendingOrRunning)
        HadoopAppDemand(1, mappersDemand, reducersDemand)
      }.reduce(_+_)
      val numSlaves = cluster.context.fold(0)(_.slaves.size)
      val supply = HadoopAppSupply(status.getMaxMapTasks * numSlaves, status.getMaxReduceTasks * numSlaves)
      ResourceStat(demand, supply, NOW)
    }
  }
}
