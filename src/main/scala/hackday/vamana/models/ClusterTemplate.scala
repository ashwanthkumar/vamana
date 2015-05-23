package hackday.vamana.models

case class ClusterTemplate(appTemplate: AppTemplate, hwConfig: HardwareConfig)

object ClusterTemplate {
  def fromSpec(spec: Map[String, String]) = {
    ClusterTemplate(
      appTemplate = AppTemplate.fromSpec(spec),
      hwConfig = HardwareConfig.fromSpec(spec)
    )
  }
}
