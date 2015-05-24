package hackday.vamana.scalar.myservice

import hackday.vamana.scalar.{Supply, Demand, ResourceStat, Collector}
import hackday.vamana.models.{ClusterStore, RunningCluster}
import org.apache.commons.httpclient.HttpClient
import com.mashape.unirest.http.Unirest
import scala.collection.JavaConverters._
import hackday.vamana.util.{Clock, VamanaLogger}
import com.mashape.unirest.request.HttpRequest
import org.jclouds.compute.domain.NodeMetadata
import hackday.vamana.service.JsonUtils


case class MyServiceDemand(quantity: Int) extends Demand {
  def +(other: MyServiceDemand) = MyServiceDemand(this.quantity + other.quantity)
}
case class MyServiceSupply(available: Int) extends Supply


class MyServiceCollector(cluster: RunningCluster, clusterStore: ClusterStore) extends Collector with VamanaLogger with Clock {
  val perNodeSupply = 100
  def stats(node: NodeMetadata) = {
    try {
      val nodeAddr = node.getPublicAddresses.asScala.filter(addr => addr != "localhost" || addr != "127.0.0.1").head
      val url = s"http://$nodeAddr:8080/status"
      Unirest.setTimeouts(30 * 1000, 30 * 1000)
      val response = Unirest.get(url).asString()
      val appMetric = JsonUtils.fromJsonAsMap(response.getBody)
      MyServiceDemand(appMetric("requests").toInt)
    }catch{case e: Exception =>
      LOG.warn("Error while fetching status")
      LOG.warn(s"!${e.getMessage}")
      MyServiceDemand(0)
    }
  }

  override def getStats: ResourceStat = {
    val updatedCluster = clusterStore.get(cluster.id)
    val nodes = updatedCluster.flatMap(_.context).fold(List[NodeMetadata]()){ ctx => ctx.master :: ctx.slaves.toList }
    val totalDemand = nodes.map(stats).reduce(_ + _)
    val totalSupply = MyServiceSupply(perNodeSupply * nodes.size)
    ResourceStat(totalDemand, totalSupply, NOW)
  }
}
