package hackday.vamana.models

case class Copy(src: String, dst: String)

case class Bootstrap(copyActions: List[Copy], commands: List[String])
case class Decommission(commands: List[String])

trait AppLifeCycle {
  def bootstrap(): Bootstrap
  def decommission(): Unit
}

// FIXME: Replace with appropriate commands & config file paths
case object HadoopLifeCycle extends AppLifeCycle {
  val HDFS_SITE_XML = ""
  val MAPRED_SITE_XML = ""
  val CORE_SITE_XML = ""
  val DECOMMISSION_SCRIPT = ""

  val REMOTE_HDFS_SITE_XML = ""
  val REMOTE_MAPRED_SITE_XML = ""
  val REMOTE_CORE_SITE_XML = ""
  val REMOTE_DECOMMISSION_SCRIPT = ""

  override def bootstrap() = {
    val copyActions = Copy(CORE_SITE_XML, REMOTE_CORE_SITE_XML) ::
      Copy(HDFS_SITE_XML, REMOTE_HDFS_SITE_XML) ::
      Copy(MAPRED_SITE_XML, REMOTE_MAPRED_SITE_XML) ::
      Copy(DECOMMISSION_SCRIPT, REMOTE_DECOMMISSION_SCRIPT) :: Nil

    Bootstrap(
      copyActions,
      List("cmd1")
    )
  }

  override def decommission() = {
    Decommission(List("cmd1"))
  }
}
