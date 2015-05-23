package hackday.vamana.service

import com.twitter.finatra.Controller
import com.twitter.util.Future
import hackday.vamana.models.{Events, ClusterStore}
import hackday.vamana.processor.RequestProcessor
import hackday.vamana.service.config.VamanaConfigReader

import scala.util.Random

class VamanaController extends Controller {
  val config = VamanaConfigReader.load

  post("/cluster/create") { request =>
    Future(render.json(s"cluster created as ${Random.nextInt()}"))
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
    // FIXME - Take a JSON payload instead
    val number = request.contentString.toInt
    RequestProcessor.process(Events.Upscale(clusterId, number))
    Future(render.json(clusterId))
  }

  put("/cluster/:id/downscale") { request =>
    val clusterId = request.routeParams("id").toLong
    // FIXME - Take a JSON payload instead
    val number = request.contentString.toInt
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

}
