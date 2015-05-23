package hackday.vamana.scalar

trait Demand {
  def quantity: Int
}

trait Supply {
  def quantity: Int
}

case class ResourceStat(demand: Demand, supply: Supply, timestamp: Long)

trait Collector {
  def getStats: ResourceStat
}
