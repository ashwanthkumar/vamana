package hackday.vamana.models

import hackday.vamana.scalar.{Scalar, Collector}

object AppTemplate {
  def fromSpec(spec: Map[String, String]) = {
    spec("app") match {
      case "hadoop" =>
        HadoopTemplate(
          props = spec,
          minNodes = spec("minNodes").toInt,
          maxNodes = spec("maxNodes").toInt
        )
      case app => throw new RuntimeException(s"We still don't know an application for $app")
    }
  }
}

case class AppContext(collector: Collector, scalar: Scalar)

trait AppTemplate {
  def maxNodes: Int

  def minNodes: Int

  def context(clusterCtx: ClusterContext): AppContext
}