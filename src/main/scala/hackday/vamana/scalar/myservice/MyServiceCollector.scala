package hackday.vamana.scalar.myservice

import hackday.vamana.scalar.{Supply, Demand, ResourceStat, Collector}
import hackday.vamana.models.RunningCluster
import org.apache.commons.httpclient.HttpClient
import com.mashape.unirest.http.Unirest
import scala.collection.JavaConverters._
import hackday.vamana.util.VamanaLogger
import com.mashape.unirest.request.HttpRequest
import org.jclouds.compute.domain.NodeMetadata
import hackday.vamana.service.JsonUtils


case class MyServiceDemand(quantity: Int) extends Demand {
  def +(other: MyServiceDemand) = MyServiceDemand(this.quantity + other.quantity)
}
case class MyServiceSupply(available: Int) extends Supply


class MyServiceCollector(cluster: RunningCluster) extends Collector with VamanaLogger {
  val perNodeSupply = 100
  def stats(node: NodeMetadata) = {
    val url = s"http://${node.getPublicAddresses}/status"
    val response = Unirest.get(url).asString()
    val appMetric = JsonUtils.fromJsonAsMap(response.getBody)
    MyServiceDemand(appMetric("requests").toInt)
  }

  override def getStats: ResourceStat = {
    val nodes = cluster.context.fold(List[NodeMetadata]()){ ctx => ctx.master :: ctx.slaves.toList }
    val totalDemand = nodes.map(stats).reduce(_ + _)
    val totalSupply = MyServiceSupply(perNodeSupply * nodes.size)
    ResourceStat(totalDemand, totalSupply, System.currentTimeMillis())
  }
}
