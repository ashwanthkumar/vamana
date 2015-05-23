package hackday.vamana.util

import org.slf4j.{LoggerFactory, Logger}

trait VamanaLogger { me =>
  val LOG = LoggerFactory.getLogger(getClass)
}
