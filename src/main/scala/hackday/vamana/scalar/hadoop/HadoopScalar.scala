package hackday.vamana.scalar.hadoop

import hackday.vamana.scalar.{ScaleUnit, ResourceStat, Scalar}
import hackday.vamana.models.{HadoopTemplate, RunningCluster}
import hackday.vamana.util.VamanaLogger

class HadoopScalar(cluster: RunningCluster) extends Scalar(cluster) with VamanaLogger {

  override def scaleUnit(normalizedStat: ResourceStat): ScaleUnit = ???

  override def downscaleCandidates(number: Int): List[String] = ???
}
