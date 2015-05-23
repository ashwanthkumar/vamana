package hackday.vamana.scalar.hadoop

import org.apache.hadoop.mapred.{JobID, JobClient, TIPStatus, TaskReport}
import org.apache.hadoop.conf.Configuration

case class HadoopJobTrackerClient(master: String, port: Int = 8021) {
  lazy val conf = { val conf_ = new Configuration(); conf_.set("mapred.job.tracker", s"$master:8021"); conf_ }
  lazy val client = new JobClient(conf)
  def pendingOrRunning(t: TaskReport) = t.getCurrentStatus == TIPStatus.PENDING || t.getCurrentStatus == TIPStatus.RUNNING
  def clusterStatus = client.getClusterStatus(true)
  def maxMapTasks = clusterStatus.getMaxMapTasks
  def maxReduceTasks = clusterStatus.getMaxReduceTasks
  def mappersInUse(id: JobID) = client.getMapTaskReports(id).count(pendingOrRunning)
  def reducersInUse(id: JobID) = client.getReduceTaskReports(id).count(pendingOrRunning)
  def runningJobs = client.jobsToComplete()
}
