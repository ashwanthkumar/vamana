package org.apache.hadoop.mapred

import java.io.PrintWriter
import java.util.Date
import java.util.concurrent.TimeUnit.{DAYS, MINUTES}
import java.util.concurrent.{Executors, ScheduledFuture}

import org.apache.commons.lang.time.{DurationFormatUtils, StopWatch}
import org.apache.hadoop.conf.Configuration

case class Supply(maps: Int, reducers: Int)

case class Demand(maps: Int, reducers: Int) {
  def +(other: Demand) = this.copy(maps = maps + other.maps, reducers = reducers + other.reducers)
}

case class ResourceStat(supply: Supply, demand: Demand, activeJobs: Int, timestamp: Long)

object Util {
  def timed[T](body: => T) = {
    val watch = new StopWatch
    watch.start()
    val result = body
    watch.stop()
    println("Time took = " + DurationFormatUtils.formatDurationHMS(watch.getTime))
    result
  }
}

class ClusterStatsCollector(writer: PrintWriter) extends Runnable {
  var count = 0

  override def run(): Unit = {
    println(s"${new Date().toString} Collecting Resource Stats on the cluster")
    Util.timed {
      val conf = new Configuration
      // conf.set("mapred.job.tracker", "hadoop-jt.node:8021")
      val client = new JobClient(conf)
      val status = client.getClusterStatus(true)
      println("active TTs = " + status.getTaskTrackers)
      println("blacklisted TTs = " + status.getBlacklistedTrackers)
      println("active map tasks = " + status.getMapTasks)
      println("total map tasks = " + status.getMaxMapTasks)
      println("active reduce tasks = " + status.getReduceTasks)
      println("total reduce tasks = " + status.getMaxReduceTasks)

      val supply = Supply(status.getMaxMapTasks, status.getMaxReduceTasks)
      val activeJobs = client.jobsToComplete()
      val demand = activeJobs.map { status =>
        val id = status.getJobID
        val mappersDemand = client.getMapTaskReports(id).filter(t => t.getCurrentStatus == TIPStatus.PENDING || t.getCurrentStatus == TIPStatus.RUNNING)
        val reducersDemand = client.getReduceTaskReports(id).filter(t => t.getCurrentStatus == TIPStatus.PENDING || t.getCurrentStatus == TIPStatus.RUNNING)
        println(s"${jobName(id)} with mapProgress=${status.mapProgress()}, reduceProgress=${status.reduceProgress()}, mappersDemand=${mappersDemand.length}, reducersDemand=${reducersDemand.length}")
        Demand(mappersDemand.length, reducersDemand.length)
      }.reduce(_ + _)

      println("Mappers in Demand = " + demand.maps)
      println("Reducers in Demand = " + demand.reducers)

      val stat = ResourceStat(supply, demand, activeJobs.length, System.currentTimeMillis())
      writer.println(toTsv(stat))
      writer.flush()
    }
    count += 1
  }

  def jobName(id: JobID) = {
    s"job_${id.getJtIdentifier}_${id.getId}"
  }

  def toTsv(stat: ResourceStat) = {
    s"${stat.supply.maps},${stat.supply.reducers},${stat.demand.maps},${stat.demand.reducers},${stat.activeJobs},${stat.timestamp}"
  }
}

class StatStopper(thread: ScheduledFuture[_], writer: PrintWriter) extends Runnable {
  override def run(): Unit = {
    thread.cancel(false)
    writer.close()
  }
}

object Main extends App {
  val outputFile = args(0)
  val writer = new PrintWriter(outputFile)

  val executor = Executors.newScheduledThreadPool(1)
  val statCollector = new ClusterStatsCollector(writer)
  val stat = executor.scheduleAtFixedRate(statCollector, 0, 1, MINUTES)
  executor.schedule(new StatStopper(stat, writer), 10, DAYS)
  stat.get() // wait until we're done
  println(s"Done collecting the stats")
}
