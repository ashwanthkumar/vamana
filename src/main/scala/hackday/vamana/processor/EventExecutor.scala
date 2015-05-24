package hackday.vamana.processor

import hackday.vamana.models._
import hackday.vamana.provisioner.ClusterProvisioner
import hackday.vamana.util.VamanaLogger
import org.apache.commons.lang.time.{DurationFormatUtils, StopWatch}

class EventExecutor(event: Event, store: ClusterStore) extends Runnable with VamanaLogger {

  import hackday.vamana.models.Events._

  def handleCreate(uniqueClusterId: Long, clusterSpec: ClusterSpec) = {
    val watch = new StopWatch
    watch.start()
    LOG.info(s"Starting to create cluster with $clusterSpec")
    val initializingCluster = RunningCluster(uniqueClusterId, clusterSpec, Booting, None)
    store.save(initializingCluster)

    try {
      val clusterContext = ClusterProvisioner.create(clusterSpec)
      val runningCluster = initializingCluster.copy(context = Some(clusterContext), status = Running)
      LOG.info(s"Started cluster ${clusterSpec.name}(${runningCluster.id})")
      LOG.info(s"Master - ${clusterContext.master.getPublicAddresses} / ${clusterContext.master.getPrivateAddresses}")
      clusterContext.slaves.foreach(slave => LOG.info(s"Started slave - ${slave.getPublicAddresses} / ${slave.getPrivateAddresses}"))
      store.save(runningCluster)
      LOG.info(s"Cluster ${clusterSpec.name} has been created in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")

      val appContext = clusterSpec.appTemplate.context(runningCluster, store)

      val bootstrapAction = appContext.lifeCycle.bootstrap()
      ClusterProvisioner.bootstrap(clusterSpec, clusterContext, bootstrapAction)

      LOG.info(s"Registering collector for ${clusterSpec.name}")
      RequestProcessor.startCollector(runningCluster, appContext.collector)

      LOG.info(s"Registering AutoScaling for ${clusterSpec.name}")
      RequestProcessor.startAutoScalar(runningCluster.id, appContext.scalar)

    } catch {
      case e: Exception =>
        LOG.error(e.getMessage, e)
        store.save(initializingCluster.copy(status = Failed, context = None))
    }

  }

  override def run(): Unit = {
    event match {
      case c @ Create(spec, uniqueClusterId) =>
        val clusterSpec = ClusterSpec.fromSpec(spec)
        handleCreate(uniqueClusterId, clusterSpec)

      case Upscale(clusterId, factor) =>
        LOG.info(s"Starting to upscale the cluster=$clusterId with $factor nodes")
        val watch = new StopWatch
        watch.start()
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          try {
            store.save(cluster.copy(status = Upscaling))
            val appContext = cluster.spec.appTemplate.context(cluster, store)
            val updatedCluster = ClusterProvisioner.upScale(cluster, appContext, factor)
            store.save(updatedCluster.copy(status = Running))
            watch.stop()
          } catch {
            case e: Exception =>
              LOG.error(e.getMessage, e)
              store.save(cluster.copy(status = Failed, context = None))
          }
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
          store.save(cluster.copy(status = Downscaling))
          try {
            val appContext = cluster.spec.appTemplate.context(cluster, store)
            val newContext = ClusterProvisioner.downScale(cluster.spec, context, appContext.scalar, factor)
            store.save(cluster.copy(context = Some(newContext), status = Running))
            LOG.info(s"Downscaled the ${cluster.spec.name} by $factor nodes in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")
          } catch {
            case e: Exception =>
              LOG.error(e.getMessage, e)
              store.save(cluster.copy(status = Failed, context = None))
          }
        }

      case Stop(clusterId) =>
        val watch = new StopWatch
        watch.start()
        val clusterOption = store.get(clusterId)
        for(
          cluster <- clusterOption;
          context <- cluster.context
        ){
          LOG.info(s"Stopping stats collector for ${cluster.spec.name}")
          RequestProcessor.stopCollector(cluster.id)
          LOG.info(s"Stats collector has stopped for ${cluster.spec.name}")

          LOG.info(s"Stopping AutoScaling for ${cluster.spec.name}")
          RequestProcessor.stopAutoScalar(cluster.id)
          LOG.info(s"AutoScaling hast stopped for ${cluster.spec.name}")

          store.save(cluster.copy(status = Stopping))
          ClusterProvisioner
        }

      case Teardown(clusterId) =>
        val watch = new StopWatch
        watch.start()
        val clusterOption = store.get(clusterId)
        for (
          cluster <- clusterOption;
          context <- cluster.context
        ) {
          try {
            LOG.info(s"Stopping stats collector for ${cluster.spec.name}")
            RequestProcessor.stopCollector(cluster.id)
            LOG.info(s"Stats collector has stopped for ${cluster.spec.name}")

            LOG.info(s"Stopping AutoScaling for ${cluster.spec.name}")
            RequestProcessor.stopAutoScalar(cluster.id)
            LOG.info(s"AutoScaling hast stopped for ${cluster.spec.name}")

            store.save(cluster.copy(status = Terminating))
            ClusterProvisioner.tearDown(cluster.spec, context)
            store.save(cluster.copy(status = NotRunning, context = None))
            LOG.info(s"Cluster ${cluster.spec.name} has been terminated in ${DurationFormatUtils.formatDurationHMS(watch.getTime)}")
          } catch {
            case e: Exception =>
              LOG.error(e.getMessage, e)
              store.save(cluster.copy(status = Failed, context = None))
          }
        }

      case DoNothing => /* do nothing */
    }
  }

}
