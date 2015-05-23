package hackday.vamana.util

import org.apache.log4j.Logger


trait VamanaLogger { me => 
  val LOG = Logger.getLogger(me.getClass.getCanonicalName)
}
