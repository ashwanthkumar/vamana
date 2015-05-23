package hackday.vamana.service

import com.twitter.finatra.FinatraServer

object VamanaService extends FinatraServer {
  register(new VamanaController)
}
