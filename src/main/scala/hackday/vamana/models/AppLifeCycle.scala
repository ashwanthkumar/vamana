package hackday.vamana.models

import com.google.common.io.Files
import hackday.vamana.scalar.hadoop.ConfigurationGenerator

case class Copy(src: String, dst: String)

case class Bootstrap(copyActions: List[Copy], commands: List[String])

case class Decommission(commands: List[String])

trait AppLifeCycle {
  def bootstrap(): Bootstrap

  def decommission(): Unit
}

// FIXME: Replace with appropriate commands & config file paths
case class HadoopLifeCycle(clusterContext: ClusterContext, props: Map[String, String]) extends AppLifeCycle {
  val HDFS_SITE_XML = "hdfs-site.xml"
  val MAPRED_SITE_XML = "mapred-site.xml"
  val CORE_SITE_XML = "core-site.xml"
  val DECOMMISSION_SCRIPT = ""

  val REMOTE_HDFS_SITE_XML = "/tmp/hdfs-site.xml"
  val REMOTE_MAPRED_SITE_XML = "/tmp/mapred-site.xml"
  val REMOTE_CORE_SITE_XML = "/tmp/core-site.xml"
  val REMOTE_DECOMMISSION_SCRIPT = ""

  override def bootstrap() = {
    val baseOutput = Files.createTempDir()
    new ConfigurationGenerator(clusterContext).createConfigurations(baseOutput)

    val copyActions = List(
      Copy(s"${baseOutput.getAbsolutePath}/$CORE_SITE_XML", REMOTE_CORE_SITE_XML),
      Copy(s"${baseOutput.getAbsolutePath}/$HDFS_SITE_XML", REMOTE_HDFS_SITE_XML),
      Copy(s"${baseOutput.getAbsolutePath}/$MAPRED_SITE_XML", REMOTE_MAPRED_SITE_XML)
    )
    // Copy(s"${baseOutput.getAbsolutePath}/$DECOMMISSION_SCRIPT", REMOTE_DECOMMISSION_SCRIPT) :: Nil

    Bootstrap(
      copyActions,
      List(
        """echo 'Showing core-site.xml'""",
        """cat /tmp/core-site.xml"""
      )
    )
  }

  override def decommission() = {
    Decommission(List("cmd1"))
  }
}
