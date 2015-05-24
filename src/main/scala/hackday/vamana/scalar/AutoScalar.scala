package hackday.vamana.scalar

import hackday.vamana.models.Events.{DoNothing, Downscale, Upscale}
import hackday.vamana.processor.RequestProcessor
import hackday.vamana.util.VamanaLogger

case class AutoScaleConfig(hustlePeriod: Long, upscaleBurstRate: Double, downscaleBurstRate: Double)

object AutoScaleConfig {
  def apply() = AutoScaleConfig(1000, 100.0, 25.0)
}

class AutoScalar(appScalar: Scalar, config: AutoScaleConfig, metricsStore: MetricStore, clusterId: Long) extends Runnable with VamanaLogger {
  var lastCheckTime: Long = 0

  override def run(): Unit = {
    if (lastCheckTime < 1) {
      lastCheckTime = System.currentTimeMillis()
    } else if (System.currentTimeMillis() - lastCheckTime >= config.hustlePeriod) {
      val stats = metricsStore.get(clusterId)
      stats match {
        case latest :: rest =>
          val scaleUnit = appScalar.scaleUnit(latest)
          RequestProcessor.processEvent(createScaleEvent(scaleUnit.numberOfNodes))
        case Nil =>
          LOG.warn(s"$clusterId has no metrics collected yet")
      }
    } else {
      // wait until hustlePeriod
    }
  }

  def createScaleEvent(n: Int) = {
    if(n > 0) Upscale(clusterId, n)
    else if (n < 0) Downscale(clusterId, n)
    else DoNothing
  }
}
