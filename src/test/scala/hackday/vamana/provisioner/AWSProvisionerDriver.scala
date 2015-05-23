package hackday.vamana.provisioner

import hackday.vamana.models._

object AWSProvisionerDriver {

  //  "AWSProvisioner" should "create N instances on addNodes" in {
  def main(args: Array[String]) {
    val (accessKey, secretKey) = (System.getenv("EC2_ACCESS_KEY"), System.getenv("EC2_SECRET_KEY"))
    val hadoopCluster = ClusterSpec("vamana-hadoop-test",
      HadoopTemplate(Map[String, String](), 2, 3),
      AWSHardwareConfig(accessKey, secretKey, "m3.xlarge")
    )
    val clusterContext = ClusterProvisioner.create(hadoopCluster)
    println(clusterContext)

    val results = ClusterProvisioner.runScriptOn(hadoopCluster, clusterContext, "ls")
    results.foreach{r =>
      println("--------- Results ----------")
      println(r)
    }

    println("Triggering cluster shutdown...")
    ClusterProvisioner.tearDown(hadoopCluster, clusterContext)

  }
}
