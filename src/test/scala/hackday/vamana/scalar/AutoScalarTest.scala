package hackday.vamana.scalar

import hackday.vamana.models.Events.{DoNothing, Downscale, Upscale}
import hackday.vamana.models.InMemoryClusterStore
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{convertToAnyShouldWrapper, be}
import org.mockito.Mockito._

class AutoScalarTest extends FlatSpec {
  val autoScalar = new AutoScalar(mock(classOf[Scalar]), AutoScaleConfig(), InMemoryMetricStore.getInstance, 1, InMemoryClusterStore.getInstance)
  "AutoScalar" should "create right scale event for upscale" in {
    autoScalar.createScaleEvent(5, 7, 1, 5) should be(Upscale(1, 2))
    autoScalar.createScaleEvent(5, 10, 1, 5) should be(Upscale(1, 5))
    autoScalar.createScaleEvent(5, 5, 1, 5) should be(DoNothing)
  }

  it should "create right scale event for downscale" in {
    autoScalar.createScaleEvent(-5, 10, 1, 5) should be(Downscale(1, 4))
    autoScalar.createScaleEvent(-5, 10, 1, 6) should be(Downscale(1, 5))
    autoScalar.createScaleEvent(-5, 10, 1, 1) should be(DoNothing)
  }

}
