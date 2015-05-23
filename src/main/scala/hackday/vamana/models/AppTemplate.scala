package hackday.vamana.models

object AppTemplate {
  def fromSpec(spec: Map[String, String]) = {
    spec("app") match {
      case "hadoop" =>
        HadoopTemplate(
          props = spec,
          minNodes = spec.getOrElse("minNodes", "1").toInt,
          maxNodes = spec.getOrElse("maxNodes", "2").toInt
        )
      case app => throw new RuntimeException(s"We still don't know an application for $app")
    }
  }
}

trait AppTemplate {
  def maxNodes: Int

  def minNodes: Int
}