package hackday.vamana.models

case class ClusterSpec(name: String, appTemplate: AppTemplate, hwConfig: HardwareConfig)

object ClusterSpec {
  def fromSpec(spec: Map[String, String]) = {
    ClusterSpec(
      name = spec("name"),
      appTemplate = AppTemplate.fromSpec(spec),
      hwConfig = HardwareConfig.fromSpec(spec)
    )
  }
}
