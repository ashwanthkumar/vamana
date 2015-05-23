package hackday.vamana.models

case class ClusterTemplate(appConfig: AppTemplate, hwConfig: HardwareConfig)

object ClusterTemplate {
  def fromSpec(spec: Map[String, String]) = {
    ClusterTemplate(
      appConfig = AppTemplate.fromSpec(spec),
      hwConfig = HardwareConfig.fromSpec(spec)
    )
  }
}
