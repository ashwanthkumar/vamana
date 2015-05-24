[![Build Status](https://snap-ci.com/ashwanthkumar/vamana/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/vamana/branch/master)

# vamana
Vamana is an auto scalar that helps you scale your software or hardware based on application demand. Making Elasticity truely elastic.  

## How does it work?
Vamana has an Autoscaling algorithm which identifies based on Demand-Supply model if a given application has to Upscale / Downscale. In order to integrate your application with Vamana it needs to implement what are called as a "Collector" and a "Scalar". 

### Collector
```scala
case class ResourceStat(demand: Demand, supply: Supply, timestamp: Long)

trait Collector {
  def getStats: ResourceStat
}
```
Collector is used for collecting stats about the application in a `Demand-Supply` fashion. `Demand` represents how much unit of work is being done / still pending while `Supply` represents how much capacity is present in the application. 

### Scalar
Scalar is used to calculate given a normalised demand and supply. How much nodes are required to satisfy the demand / how much nodes to be downscaled. 
```scala
case class ScaleUnit(numberOfNodes: Int)

abstract class Scalar(cluster: RunningCluster) {
  def scaleUnit(normalizedStat: ResourceStat): ScaleUnit

  def downscaleCandidates(number: Int): List[String]
}
```
Scalar is also used to determine which are all the nodes which has to be downscaled. It could be based on oldestRunning / latestRunning / machineWithLowestDiskSpace / machineWithLowestMemory, etc.
