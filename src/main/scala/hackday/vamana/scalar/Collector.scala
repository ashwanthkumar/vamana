package hackday.vamana.scalar

trait Demand {
  def needed: Int
}

trait Supply {
  def available: Int
}

case class ResourceStat(demand: Demand, supply: Supply, timestamp: Long)

trait Collector {
  def getStats: ResourceStat
}
