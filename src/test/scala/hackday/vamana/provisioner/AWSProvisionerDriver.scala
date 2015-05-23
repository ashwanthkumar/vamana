package hackday.vamana.provisioner

import hackday.vamana.models._
import hackday.vamana.processor.EventExecutor
import hackday.vamana.models.Events.Create

object AWSProvisionerDriver {

  def setupAndTeardownHadoopCluster(accessKey: String, secretKey: String) = {
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

  def setupAndTeardownMyService(accessKey: String, secretKey: String) = {
    val myServiceClusterSpec = ClusterSpec("vamana-myservice-test",
      MyServiceTemplate(Map[String, String](), 2, 3),
      AWSHardwareConfig(accessKey, secretKey, "m3.xlarge")
    )

    val clusterContext = ClusterProvisioner.create(myServiceClusterSpec)
    println(clusterContext)

    val appContext = myServiceClusterSpec.appTemplate.context(RunningCluster(1, myServiceClusterSpec, Running, Some(clusterContext)))

    ClusterProvisioner.bootstrap(myServiceClusterSpec, clusterContext, appContext.lifeCycle.bootstrap())

    (0 to 10).foreach{ i =>
      val resourceStat = appContext.collector.getStats
      println(resourceStat)
      Thread.sleep(30)
    }


    println("Triggering cluster shutdown...")
    ClusterProvisioner.tearDown(myServiceClusterSpec, clusterContext)

    ClusterProvisioner.shutdown
  }


  //  "AWSProvisioner" should "create N instances on addNodes" in {
  def main(args: Array[String]) {
    val (accessKey, secretKey) = (System.getenv("EC2_ACCESS_KEY"), System.getenv("EC2_SECRET_KEY"))
    setupAndTeardownMyService(accessKey, secretKey)
  }
}
