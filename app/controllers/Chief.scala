package controllers

import play.api.mvc.{Controller, Action}
import play.api.libs.iteratee.Enumerator
import java.text.DecimalFormat
import org.jsoup.Jsoup
import dispatch._
import org.jsoup.nodes.Element
import play.api.libs.json.{JsArray, JsString, JsNumber, Json}
import org.jsoup.helper.StringUtil
import scala._
import play.api.mvc.ResponseHeader
import play.api.mvc.SimpleResult
import org.stringtemplate.v4.ST
import scala.collection.JavaConversions._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger

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
      val pagingChief: String = getUrlFrom(req.queryString)
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
      SimpleResult(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
        body = Enumerator(Json.stringify(JsArray(values)).getBytes)
      )
  }

  def getUrlFrom(params: Map[String, Seq[String]]) = {
    val template = new ST(uri)
    params.foreach {
      case (name: String, values: Seq[String]) => values.foreach(template.add(name, _))
    }
    template.render
  }

  val fakeData = """[{"action":"/takeEditingOwnership.do","type":"min","y":10620,"x":"12/03/2013 18:00"},
                   |{"action":"/takeEditingOwnership.do","type":"min","y":705,"x":"12/03/2013 17:00"},
                   |{"action":"/takeEditingOwnership.do","type":"min","y":1021,"x":"12/03/2013 16:00"},
                   |{"action":"/takeEditingOwnership.do","type":"avg","y":10620,"x":"12/03/2013 18:00"},
                   |{"action":"/takeEditingOwnership.do","type":"avg","y":705,"x":"12/03/2013 17:00"},
                   |{"action":"/takeEditingOwnership.do","type":"avg","y":7477.33,"x":"12/03/2013 16:00"},
                   |{"action":"/takeEditingOwnership.do","type":"max","y":10620,"x":"12/03/2013 18:00"},
                   |{"action":"/takeEditingOwnership.do","type":"max","y":705,"x":"12/03/2013 17:00"},
                   |{"action":"/takeEditingOwnership.do","type":"max","y":12533,"x":"12/03/2013 16:00"},
                   |{"action":"/takeEditingOwnership.do","type":"count","y":1,"x":"12/03/2013 18:00"},
                   |{"action":"/takeEditingOwnership.do","type":"count","y":1,"x":"12/03/2013 17:00"},
                   |{"action":"/takeEditingOwnership.do","type":"count","y":3,"x":"12/03/2013 16:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":4053,"x":"12/04/2013 02:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":1003,"x":"12/04/2013 00:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":795,"x":"12/03/2013 23:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":812,"x":"12/03/2013 22:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":1344,"x":"12/03/2013 21:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":1429,"x":"12/03/2013 20:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":1335,"x":"12/03/2013 18:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":1798,"x":"12/03/2013 17:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":1942,"x":"12/03/2013 16:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"min","y":2397,"x":"12/03/2013 14:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":4155.5,"x":"12/04/2013 02:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":2321.5,"x":"12/04/2013 00:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":2657,"x":"12/03/2013 23:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":1864.14,"x":"12/03/2013 22:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":2132.67,"x":"12/03/2013 21:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":2426,"x":"12/03/2013 20:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":3792.67,"x":"12/03/2013 18:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":7599.3,"x":"12/03/2013 17:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":5159.08,"x":"12/03/2013 16:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"avg","y":3382.67,"x":"12/03/2013 14:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":4258,"x":"12/04/2013 02:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":3640,"x":"12/04/2013 00:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":3737,"x":"12/03/2013 23:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":3303,"x":"12/03/2013 22:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":3320,"x":"12/03/2013 21:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":4311,"x":"12/03/2013 20:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":6494,"x":"12/03/2013 18:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":21839,"x":"12/03/2013 17:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":21738,"x":"12/03/2013 16:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"max","y":5222,"x":"12/03/2013 14:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":2,"x":"12/04/2013 02:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":2,"x":"12/04/2013 00:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":4,"x":"12/03/2013 23:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":7,"x":"12/03/2013 22:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":3,"x":"12/03/2013 21:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":4,"x":"12/03/2013 20:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":9,"x":"12/03/2013 18:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":10,"x":"12/03/2013 17:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":24,"x":"12/03/2013 16:00"},
                   |{"action":"/editProductSettingsAjax.do","type":"count","y":3,"x":"12/03/2013 14:00"},
                   |{"action":"/editProduct.do","type":"min","y":22604,"x":"12/04/2013 02:00"},
                   |{"action":"/editProduct.do","type":"min","y":1550,"x":"12/04/2013 00:00"},
                   |{"action":"/editProduct.do","type":"min","y":1063,"x":"12/03/2013 23:00"},
                   |{"action":"/editProduct.do","type":"min","y":1407,"x":"12/03/2013 22:00"},
                   |{"action":"/editProduct.do","type":"min","y":20822,"x":"12/03/2013 21:00"},
                   |{"action":"/editProduct.do","type":"min","y":2114,"x":"12/03/2013 20:00"},
                   |{"action":"/editProduct.do","type":"min","y":17896,"x":"12/03/2013 18:00"},
                   |{"action":"/editProduct.do","type":"min","y":15190,"x":"12/03/2013 17:00"},
                   |{"action":"/editProduct.do","type":"min","y":4104,"x":"12/03/2013 16:00"},
                   |{"action":"/editProduct.do","type":"min","y":16483,"x":"12/03/2013 15:00"},
                   |{"action":"/editProduct.do","type":"avg","y":22604,"x":"12/04/2013 02:00"},
                   |{"action":"/editProduct.do","type":"avg","y":1550,"x":"12/04/2013 00:00"},
                   |{"action":"/editProduct.do","type":"avg","y":7031.6,"x":"12/03/2013 23:00"},
                   |{"action":"/editProduct.do","type":"avg","y":12895,"x":"12/03/2013 22:00"},
                   |{"action":"/editProduct.do","type":"avg","y":20822,"x":"12/03/2013 21:00"},
                   |{"action":"/editProduct.do","type":"avg","y":8009.33,"x":"12/03/2013 20:00"},
                   |{"action":"/editProduct.do","type":"avg","y":17896,"x":"12/03/2013 18:00"},
                   |{"action":"/editProduct.do","type":"avg","y":25237.25,"x":"12/03/2013 17:00"},
                   |{"action":"/editProduct.do","type":"avg","y":17727.25,"x":"12/03/2013 16:00"},
                   |{"action":"/editProduct.do","type":"avg","y":16483,"x":"12/03/2013 15:00"},
                   |{"action":"/editProduct.do","type":"max","y":22604,"x":"12/04/2013 02:00"},
                   |{"action":"/editProduct.do","type":"max","y":1550,"x":"12/04/2013 00:00"},
                   |{"action":"/editProduct.do","type":"max","y":9668,"x":"12/03/2013 23:00"},
                   |{"action":"/editProduct.do","type":"max","y":28789,"x":"12/03/2013 22:00"},
                   |{"action":"/editProduct.do","type":"max","y":20822,"x":"12/03/2013 21:00"},
                   |{"action":"/editProduct.do","type":"max","y":16557,"x":"12/03/2013 20:00"},
                   |{"action":"/editProduct.do","type":"max","y":17896,"x":"12/03/2013 18:00"},
                   |{"action":"/editProduct.do","type":"max","y":30314,"x":"12/03/2013 17:00"},
                   |{"action":"/editProduct.do","type":"max","y":35509,"x":"12/03/2013 16:00"},
                   |{"action":"/editProduct.do","type":"max","y":16483,"x":"12/03/2013 15:00"},
                   |{"action":"/editProduct.do","type":"count","y":1,"x":"12/04/2013 02:00"},
                   |{"action":"/editProduct.do","type":"count","y":1,"x":"12/04/2013 00:00"},
                   |{"action":"/editProduct.do","type":"count","y":5,"x":"12/03/2013 23:00"},
                   |{"action":"/editProduct.do","type":"count","y":3,"x":"12/03/2013 22:00"},
                   |{"action":"/editProduct.do","type":"count","y":1,"x":"12/03/2013 21:00"},
                   |{"action":"/editProduct.do","type":"count","y":3,"x":"12/03/2013 20:00"},
                   |{"action":"/editProduct.do","type":"count","y":1,"x":"12/03/2013 18:00"},
                   |{"action":"/editProduct.do","type":"count","y":4,"x":"12/03/2013 17:00"},
                   |{"action":"/editProduct.do","type":"count","y":8,"x":"12/03/2013 16:00"},
                   |{"action":"/editProduct.do","type":"count","y":1,"x":"12/03/2013 15:00"}]""".stripMargin

  def fake() = Action {
    implicit req =>
      SimpleResult(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
        body = Enumerator(fakeData.getBytes)
      )
  }
}