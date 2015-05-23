package hackday.vamana.models

case class Credentials(identity: String, credential: String)

trait HardwareConfig {
  def provider: String

  def credentials: Credentials
}

object HardwareConfig {
  def fromSpec(spec: Map[String, String]) = {
    spec("cloud") match {
      case "aws" =>
        val config = AWSHardwareConfig(
          accessKeyId = spec("accessId"),
          secretKeyId = spec("secretKey"),
          ami = spec.getOrElse("ami", "foo-bar-ami"),
          instanceType = spec.getOrElse("machineType", "t1.micro")
        )

        spec.get("spot-price").map(price => config.copy(spotPrice = Some(price.toDouble))).getOrElse(config)
      case cloud => throw new RuntimeException(s"We still don't support $cloud cloud provider")
    }
  }
}
