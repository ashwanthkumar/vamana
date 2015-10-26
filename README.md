# UPDATE
This project is deprecated in favour of [vamana2](https://github.com/ashwanthkumar/vamana2). Please refer there for the latest version.

<hr />

[![Build Status](https://snap-ci.com/ashwanthkumar/vamana/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/vamana/branch/master)

# vamana
Vamana is an auto scalar that helps you scale your software or hardware based on application demand. Making Elasticity truely elastic.  

## How does it work?
Vamana has an Autoscaling algorithm which identifies your application load based on a Demand-Supply model and determine if it has to Upscale / Downscale. In order to integrate your application with Vamana it needs to implement what are called as a "Collector" and a "Scalar". 

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

### API
Vamana runs as a simple HTTP service that exposes the following endpoints

| Action | API Resource | Comments |
|------ | ------------ | -------- |
| POST      | /cluster/create       |   Create a new cluster with an application and return the clusterId |
| GET       | /cluster/:id          |   Returns the cluster information |
| PUT       | /cluster/:id/start    |   Start the given cluster if its not running  |
| PUT       | /cluster/:id/stop     |   Stop the given cluster by terminating all the running instances |
| DELETE    | /cluster/:id          |   Delete the given cluster instance and all the metadata associated with it.  |
| PUT       | /cluster/:id/upscale  |   Upscale the cluster with `nodes` (query param) number of nodes  |
| PUT       | /cluster/:id/downscale|   Downscale the cluster with `nodes` (query param) number of nodes  |

You can find a Postman Collection under the root of the repo as [`postmanCollection.json`](https://github.com/ashwanthkumar/vamana/blob/master/postmanCollection.json).
