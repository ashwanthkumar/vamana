package hackday.vamana.scalar.myservice

import hackday.vamana.scalar.{Supply, Demand, ResourceStat, Collector}
import hackday.vamana.models.RunningCluster
import org.apache.commons.httpclient.HttpClient
import com.mashape.unirest.http.Unirest
import scala.collection.JavaConverters._
import hackday.vamana.util.VamanaLogger
import com.mashape.unirest.request.HttpRequest
import org.jclouds.compute.domain.NodeMetadata


case class MyServiceDemand(needed: Int) extends Demand
case class MyServiceSupply(available: Int) extends Supply


class MyServiceCollector(cluster: RunningCluster) extends Collector with VamanaLogger {
  def stats(node: NodeMetadata) = {
    val url = s"http://${node.getPublicAddresses}/status"
    Unirest.get(url).asJson()
  }

  override def getStats: ResourceStat = {
    val nodes = cluster.context.fold(List[NodeMetadata]()){ ctx => ctx.master :: ctx.slaves.toList }
    ???
  }
}
