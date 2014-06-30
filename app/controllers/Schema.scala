package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import scala.xml.Source
import play.api.mvc.ResponseHeader
import play.api.mvc.Result
import helpers.SchemaXMLParser
import play.api.Play._

/**
 * Schema 
 *
 * @author <a href="mailto:kchen@digitalriver.com">Ken Chen</a>
 */
object Schema extends Controller {

  def index = Action {
    Ok(views.html.schema("CAST related Table Schema."))
  }

  def json = Action {
    val source = resourceAsStream("/schema.xml") match {
      case Some(schema) => Option(Source.fromInputStream(schema))
      case _ => None
    }
    Result(
      header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
      body = Enumerator(Json.prettyPrint(SchemaXMLParser.parse(source)).getBytes)
    )
  }

}
