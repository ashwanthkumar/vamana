package hackday.vamana.provisioner

import hackday.vamana.models._
import org.scalatest.FlatSpec

object AWSProvisionerDriver {

//  "AWSProvisioner" should "create N instances on addNodes" in {
  def main(args: Array[String]) {
    val (accessKey, secretKey) = (System.getenv("EC2_ACCESS_KEY"), System.getenv("EC2_SECRET_KEY"))
    val hadoopCluster = Cluster(123l, "vamana-hadoop-test", ClusterTemplate(
      HadoopTemplate(Map[String,String](), 2, 3),
      AWSHardwareConfig(accessKey, secretKey, "", "m3.xlarge")
    ), NotRunning)
    ClusterProvisioner.create(hadoopCluster)
  }
}
