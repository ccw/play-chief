package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import scala.xml.Source
import play.api.mvc.ResponseHeader
import play.api.mvc.SimpleResult
import org.xml.sax.InputSource
import helpers.SchemaXMLParser

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
    val source: InputSource = Source.fromInputStream(Schema.getClass.getResourceAsStream("/schema.xml"))
    SimpleResult(
      header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
      body = Enumerator(Json.prettyPrint(SchemaXMLParser.parse(source)).getBytes)
    )
  }

}
