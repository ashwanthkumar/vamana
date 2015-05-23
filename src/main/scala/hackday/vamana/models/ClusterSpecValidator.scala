package hackday.vamana.models

object ClusterSpecValidator {
  def validate(spec: Map[String, String]): Boolean = {
    // keys it must have
    // name, cloud,accessId, secretKey, minNodes, maxNodes
    // optional fields
    // ami, instanceType, spotPrice, app,

    val MUST_CONTAIN_KEYS = List("name", "cloud", "accessId", "secretKey", "minNodes", "maxNodes", "app")
    MUST_CONTAIN_KEYS.forall(spec.contains)
  }
}
