package hackday.vamana.models

case class Credentials(identity: String, credential: String)

trait HardwareConfig {
  def provider: String
  def credentials: Credentials
  def instanceType: String
  def imageId: String
}

object HardwareConfig {
  def fromSpec(spec: Map[String, String]) = {
    spec("cloud") match {
      case "aws" =>
        val config = AWSHardwareConfig(
          accessKeyId = spec("accessId"),
          secretKeyId = spec("secretKey"),
          instanceType = spec.getOrElse("instanceType", "t1.micro")
        )

        spec.get("spot-price").fold(config)(price => config.copy(spotPrice = Some(price.toDouble)))
      case cloud => throw new RuntimeException(s"We still don't support $cloud cloud provider")
    }
  }
}
