package hackday.vamana.scalar.myservice

import hackday.vamana.scalar.{ScaleUnit, ResourceStat, Scalar}
import hackday.vamana.models.RunningCluster

class MyServiceScalar(cluster: RunningCluster) extends Scalar(cluster) {
  override def scaleUnit(normalizedStat: ResourceStat): ScaleUnit = ScaleUnit(normalizedStat.demand.quantity - normalizedStat.supply.quantity)

  override def downscaleCandidates(number: Int): List[String] = {

  }
}
