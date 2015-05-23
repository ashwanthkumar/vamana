package hackday.vamana.processor

import hackday.vamana.models._
import hackday.vamana.provisioner.ClusterProvisioner
import hackday.vamana.util.VamanaLogger
import org.apache.commons.lang.time.{DurationFormatUtils, StopWatch}

class EventExecutor(event: Event, store: ClusterStore) extends Runnable with VamanaLogger {

  import hackday.vamana.models.Events._

  override def run(): Unit = {
    event match {
      case Create(spec, uniqueClusterId) =>
        val watch = new StopWatch
        watch.start()
        LOG.info(s"Starting to create cluster with ${spec.mkString(",")}")
        val clusterSpec = ClusterSpec.fromSpec(spec)
        val initializingCluster = RunningCluster(uniqueClusterId, clusterSpec, Booting, None)
        store.save(initializingCluster)

        val clusterContext = ClusterProvisioner.create(clusterSpec)
        val runningCluster = initializingCluster.copy(context = Some(clusterContext), status = Running)
        LOG.info(s"Started cluster ${clusterSpec.name}(${runningCluster.id})")
        LOG.info(s"Master - ${clusterContext.master.getPublicAddresses} / ${clusterContext.master.getPrivateAddresses}")
        clusterContext.slaves.foreach(slave => LOG.info(s"Started slave - ${slave.getPublicAddresses} / ${slave.getPrivateAddresses}"))
        store.save(runningCluster)
        LOG.info(s"Cluster ${clusterSpec.name} has been created in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")

      case Upscale(clusterId, factor) =>
        LOG.info(s"Starting to upscale the cluster=$clusterId with $factor nodes")
        val watch = new StopWatch
        watch.start()
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          ClusterProvisioner.upScale(cluster.spec, context, factor)
          watch.stop()
          LOG.info(s"Upscaled the ${cluster.spec.name} by $factor nodes in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")
        }

      case Downscale(clusterId, factor) =>
        LOG.info(s"Starting to downscale the cluster=$clusterId with $factor nodes")
        val watch = new StopWatch
        watch.start()
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          ClusterProvisioner.downScale(cluster.spec, context, factor)
          LOG.info(s"Downscaled the ${cluster.spec.name} by $factor nodes in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")
        }

      case Teardown(clusterId) =>
        val watch = new StopWatch
        watch.start()
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          ClusterProvisioner.tearDown(cluster.spec, context)
          LOG.info(s"Cluster ${cluster.spec.name} has been terminated in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")
        }
    }
  }

}
