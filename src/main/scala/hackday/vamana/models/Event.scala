package hackday.vamana.models

trait Event

object Events {

  case class Create(clusterSpec: Map[String, String], clusterId: Long) extends Event

  case class Start(id: Long) extends Event

  case class Stop(id: Long) extends Event

  case class Status(id: Long) extends Event

  case class Teardown(id: Long) extends Event

  case class Upscale(id: Long, number: Int) extends Event

  case class Downscale(id: Long, number: Int) extends Event

}