package controllers

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.Date

import dispatch._
import org.jsoup.Jsoup
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Element
import org.stringtemplate.v4.ST
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsArray, JsNumber, JsString, Json}
import play.api.mvc.{Action, Controller, ResponseHeader, Result}

import scala.collection.JavaConversions._
import scala.util.Random

object Chief extends Controller {

  val uri = "http://firechief.digitalriver.com/fcweb/jsp/graph.jsp?rangeMax=&" +
            "height=768&" +
            "rangeMin=&" +
            "width=640&" +
            "dataSourceName=<ds>&" +
            "table=drcc_page_hit&" +
            "metrics=count%281%29%3Amax%28<measure>%29%3Aavg%28<measure>%29%3Amin%28<measure>%29&" +
            "graphType=line&" +
            "xCoordinateType=time-hourly&" +
            "xCoordinate=creation_date&" +
            "xDomainType=time-<period>&" +
            "xCoordinateWhereClause=creation_date&" +
            "addlWhereClause=site_selected%3D%27<site>%27+and+%281=0<urls:{url|+or+URI='<url>'}>%29&" +
            "sequenceIdentifier=URI&" +
            "orderBy=&" +
            "having=&limit=&alpha=&chartTitle=Loading+Time"

  val dFormat: DecimalFormat = new DecimalFormat("#,###.0")

  def index = Action {
    Ok(views.html.chief("Playing with Chief."))
  }

  def graph = Action {
    implicit req =>
      val pagingChief: String = formatString(uri, req.queryString)
      Logger.debug("paging chief with " + pagingChief)
      val values = Jsoup.parse(Http(url(pagingChief) OK as.String).apply())
        .select("area").iterator.toList.map {
        case e: Element =>
          val contents = e.attr("title").split("\n")
          val heads = contents(0).split(":")
          (1 until contents.length).foldLeft(Json.obj(
            "action" -> heads(0).substring(heads(0).lastIndexOf("/")).trim,
            "type" -> heads(1).substring(0, heads(1).indexOf("(")).trim
          )) {
            (v, i) =>
              val attributes = contents(i).split("=").map(_.trim)
              val jsValue = if (StringUtil.isNumeric(attributes(1).replaceAll("[,\\.]", "")))
                JsNumber(BigDecimal(dFormat.parse(attributes(1)).doubleValue()))
              else JsString(attributes(1).substring(attributes(1).indexOf(" "), attributes(1).lastIndexOf(" ")).trim)
              v +(attributes(0).trim, jsValue)
          }
      }
      Result(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
        body = Enumerator(Json.stringify(JsArray(values)).getBytes)
      )
  }

  def formatString(templateContent:String, params: Map[String, Seq[String]]) = {
    val template = new ST(templateContent)
    params.foreach {
      case (name: String, values: Seq[String]) => values.foreach(template.add(name, _))
    }
    template.render
  }

  val fakeData = """{"action":"/takeEditingOwnership.do","type":"min","y":<min>,"x":"<time>"},
                    |{"action":"/takeEditingOwnership.do","type":"avg","y":<avg>,"x":"<time>"},
                    |{"action":"/takeEditingOwnership.do","type":"max","y":<max>,"x":"<time>"},
                    |{"action":"/takeEditingOwnership.do","type":"count","y":<count>,"x":"<time>"}""".stripMargin

  def fake() = Action {
    implicit req =>
      Result(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
        body = Enumerator(("[" + Seq(randomValues(0), randomValues(1)).mkString(",") + "]").getBytes)
      )
  }

  def randomValues(factor:Long) = formatString(fakeData, Map("min" -> Seq((Random.nextInt(10) + 1).toString),
                                                "avg" -> Seq((Random.nextInt(5) + 5).toString),
                                                "max" -> Seq((Random.nextInt(10) + 10).toString),
                                                "count" -> Seq((Random.nextInt(3) + 1).toString),
                                                "time" -> Seq(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.sss").format(new Date(System.currentTimeMillis + 1000 * factor)))))

}