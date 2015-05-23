package hackday.vamana.scalar.hadoop

import java.io.File

import com.google.common.base.Charsets
import com.google.common.io.Files
import hackday.vamana.models.ClusterContext
import hackday.vamana.util.VamanaLogger

import scala.collection.JavaConversions._

class ConfigurationGenerator(context: ClusterContext) extends VamanaLogger {
  private def generateHadoopConfigurationFile(config: Map[String, String]) = {
    val sb = new StringBuilder()
    sb.append("<?xml version=\"1.0\"?>\n")
      .append("<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>\n")
      .append("<configuration>\n")
    config.foreach { case (key, value) =>
      sb.append("  <property>\n")
        .append("    <name>").append(key).append("</name>\n")
        .append("    <value>").append(value).append("</value>\n")
        .append("  </property>\n")
    }
    sb.append("</configuration>\n").toString()
  }

  private def createClientSideHadoopSiteFile(file: File, config: Map[String, String]) {
    Files.write(generateHadoopConfigurationFile(config), file,
      Charsets.UTF_8)
    LOG.info("Wrote file {}", file)
  }

  def createConfigurations(baseOutput: File) = {
    // core-site.xml
    createClientSideHadoopSiteFile(new File(s"${baseOutput.getAbsolutePath}/core-site.xml"), Map("fs.default.name" -> s"${context.master.getPrivateAddresses.head}:9000"))
    // mapred-site.xml
    createClientSideHadoopSiteFile(new File(s"${baseOutput.getAbsolutePath}/mapred-site.xml"), Map("mapred.job.tracker" -> s"${context.master.getPrivateAddresses.head}:8021"))
    // hdfs-site.xml
    createClientSideHadoopSiteFile(new File(s"${baseOutput.getAbsolutePath}/hdfs-site.xml"), Map("dfs.data.dir" -> "/tmp/hadoop/${user.dir}"))
  }

}
