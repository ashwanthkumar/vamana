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

    ClusterProvisioner.bootstrap(hadoopCluster, clusterContext, Bootstrap(Copy("/tmp/logback.xml", "/tmp/logback_remote.xml"):: Nil, List[String]()))

    val results = ClusterProvisioner.runScriptOn(hadoopCluster, clusterContext, "ls -ltr /tmp/logback_remote.xml")
    results.foreach{r =>
      println("--------- Results ----------")
      println(s"Stdout: ${r.getOutput}")
      println(s"Stderr: ${r.getError}")
    }

    println("Triggering cluster shutdown...")
    ClusterProvisioner.tearDown(hadoopCluster, clusterContext)

    ClusterProvisioner.shutdown
  }
}
