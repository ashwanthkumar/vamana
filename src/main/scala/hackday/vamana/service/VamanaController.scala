package hackday.vamana.service

import com.twitter.finatra.{ResponseBuilder, Controller}
import com.twitter.util.Future
import hackday.vamana.models.{ClusterSpecValidator, ClusterStore, Events}
import hackday.vamana.processor.RequestProcessor
import hackday.vamana.service.config.{VamanaConfig, VamanaConfigReader}

import scala.util.Random

class VamanaController(config: VamanaConfig, clusterStore: ClusterStore) extends Controller {

  post("/cluster/create") { request =>
    val clusterSpec = JsonUtils.fromJsonAsMap(request.contentString)
    require(ClusterSpecValidator.validate(clusterSpec), "given cluster is not valid, please check the documentation on the required fields")
    val nextId = clusterStore.nextId
    RequestProcessor.process(Events.Create(clusterSpec, nextId))
    Future(respond(Map("id" -> nextId)))
  }

  get("/cluster/:id") { request =>
    val clusterId = request.routeParams("id").toLong
    val store = ClusterStore(config.clusterStoreType)
    Future(render.json(store.get(clusterId)))
  }

  put("/cluster/:id/start") { request =>
    val clusterId = request.routeParams("id").toLong
    RequestProcessor.process(Events.Start(clusterId))
    Future(render.json(clusterId))
  }

  put("/cluster/:id/stop") { request =>
    val clusterId = request.routeParams("id").toLong
    RequestProcessor.process(Events.Stop(clusterId))
    Future(render.json(clusterId))
  }

  delete("/cluster/:id") { request =>
    val clusterId = request.routeParams("id").toLong
    RequestProcessor.process(Events.Delete(clusterId))
    Future(render.json(clusterId))
  }

  put("/cluster/:id/upscale") { request =>
    val clusterId = request.routeParams("id").toLong
    val number = request.params("nodes").toInt
    RequestProcessor.process(Events.Upscale(clusterId, number))
    Future(render.json(clusterId))
  }

  put("/cluster/:id/downscale") { request =>
    val clusterId = request.routeParams("id").toLong
    val number = request.params("nodes").toInt
    RequestProcessor.process(Events.Downscale(clusterId, number))
    Future(render.json(clusterId))
  }

  error { request =>
    request.error match {
      case Some(e: Exception) =>
        log.error(e, e.getMessage)
        render.status(500).plain(e.getMessage).toFuture
      case _ =>
        render.status(500).plain("Something went wrong!").toFuture
    }
  }

  def respond(msg: String): ResponseBuilder = render.json(Map("status" -> 200, "message" -> msg))
  def respond(payload: Map[String, _]): ResponseBuilder = render.json(Map("status" -> 200) ++ payload)
}
