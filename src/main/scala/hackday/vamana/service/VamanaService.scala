package hackday.vamana.service

import com.twitter.finatra.FinatraServer
import hackday.vamana.models.ClusterStore
import hackday.vamana.service.config.VamanaConfigReader

object VamanaService extends FinatraServer {
  val config = VamanaConfigReader.load
  val clusterStore = ClusterStore(config.clusterStoreType)
  register(new VamanaController(config, clusterStore))
}
