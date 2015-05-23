package hackday.vamana.models

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

trait AppTemplate {
  def maxNodes: Int

  def minNodes: Int
}