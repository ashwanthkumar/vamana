package hackday.vamana.scalar.myservice

import hackday.vamana.scalar.{ScaleUnit, ResourceStat, Scalar}
import hackday.vamana.models.RunningCluster
import hackday.vamana.util.VamanaLogger

class MyServiceScalar(cluster: RunningCluster) extends Scalar(cluster) with VamanaLogger {
  override def scaleUnit(normalizedStat: ResourceStat): ScaleUnit = ScaleUnit(normalizedStat.demand.quantity - normalizedStat.supply.available)

  override def downscaleCandidates(number: Int): List[String] = {
    cluster.context match {
      case Some(ctx) =>
        val nodes = cluster.context.fold(List[String]())(_.slaves.take(number).map(_.getId).toList)
        LOG.info("Selecting the following nodes for Downscaling")
        LOG.info(nodes.mkString("\n"))
        nodes
      case None => LOG.warn(s"No cluster context available for this cluster: ${cluster.spec.name}. Nothing to downscale."); List[String]()
    }
  }
}
