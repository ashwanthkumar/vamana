package hackday.vamana.scalar.hadoop

import hackday.vamana.scalar.{ScaleUnit, ResourceStat, Scalar}
import hackday.vamana.models.{HadoopTemplate, RunningCluster}
import hackday.vamana.util.VamanaLogger
import scala.collection.JavaConverters._
import org.apache.hadoop.mapred.{TaskTrackerStatus, JobTrackerMXBean}

// Start with a simple approach and we can tune this further as we go forward
class HadoopScalar(cluster: RunningCluster) extends Scalar(cluster) with VamanaLogger {

  override def scaleUnit(normalizedStat: ResourceStat): ScaleUnit = {
    cluster.master.fold(ScaleUnit(0)){master =>
      val client = HadoopJobTrackerClient(master)
      val maxMapTasksPerNode = client.maxMapTasks
      val maxReduceTasksPerNode = client.maxReduceTasks
      val nodesNeeded = (normalizedStat.demand.quantity - normalizedStat.supply.available) % (maxMapTasksPerNode+maxReduceTasksPerNode)
      ScaleUnit(nodesNeeded)
    }
  }

  override def downscaleCandidates(number: Int): List[String] = {
    cluster.master.fold(List[String]()){ master =>
      val client = HadoopJobTrackerClient(master)
      val jtStatus = client.clusterStatus
      val activeTaskTrackers = jtStatus.getActiveTrackerNames
      // FIXME: Pick those TTs that have less load
      activeTaskTrackers.asScala.take(number).toList
    }
  }
}
