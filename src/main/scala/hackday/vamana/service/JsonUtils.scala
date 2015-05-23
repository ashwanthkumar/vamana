package hackday.vamana.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonUtils {

  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def toJson(o: Any) = mapper.writeValueAsString(o)

  def fromJsonAsMap(json: String) = mapper.readValue(json, classOf[Map[String, String]])
}
