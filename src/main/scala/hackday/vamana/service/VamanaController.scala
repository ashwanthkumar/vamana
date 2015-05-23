package hackday.vamana.service

import com.twitter.finatra.Controller
import com.twitter.util.Future

import scala.util.Random

class VamanaController extends Controller {
  post("/cluster/create") { request =>
    Future(render.json(s"cluster created as ${Random.nextInt()}"))
  }

  get("/cluster/:id") { request =>
    val clusterId = request.routeParams("id")
    Future(render.json(clusterId))
  }

  put("/cluster/:id/start") { request =>
    val clusterId = request.routeParams("id")
    Future(render.json(clusterId))
  }

  put("/cluster/:id/stop") { request =>
    val clusterId = request.routeParams("id")
    Future(render.json(clusterId))
  }

  delete("/cluster/:id") { request =>
    val clusterId = request.routeParams("id")
    Future(render.json(clusterId))
  }

  put("/cluster/:id/upscale") { request =>
    val clusterId = request.routeParams("id")
    Future(render.json(clusterId))
  }

  put("/cluster/:id/downscale") { request =>
    val clusterId = request.routeParams("id")
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
