package hackday.vamana.models

trait ClusterStatus {}

case object NotRunning extends ClusterStatus
case object Booting extends ClusterStatus
case object Running extends ClusterStatus
case object Terminating extends ClusterStatus
case object Failed extends ClusterStatus
