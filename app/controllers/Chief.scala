package controllers

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.{Locale, Calendar, Date, TimeZone}

import dispatch._
import org.jsoup.Jsoup
import org.jsoup.helper.StringUtil
import org.jsoup.nodes.Element
import org.stringtemplate.v4.ST
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsArray, JsNumber, JsString, Json}
import play.api.mvc._

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
        "xCoordinateType=time-minutely&" +
        "xCoordinate=creation_date&" +
        "xDomainType=time-lasthour&" +
        "xCoordinateWhereClause=creation_date&" +
        "addlWhereClause=site_selected%3D%27<site>%27+and+%281=0<urls:{url|+or+URI='<url>'}>%29&" +
        "sequenceIdentifier=URI&" +
        "orderBy=&" +
        "having=&limit=&alpha=&chartTitle=Loading+Time"

    val dataTemplate = """{"action":"<action>","type":"min","y":<min>,"x":"<time>"},
                         |{"action":"<action>","type":"avg","y":<avg>,"x":"<time>"},
                         |{"action":"<action>","type":"max","y":<max>,"x":"<time>"},
                         |{"action":"<action>","type":"count","y":<count>,"x":"<time>"}""".stripMargin

    val dFormat = new DecimalFormat("#,###.0")
    val tInFormat = new SimpleDateFormat("EEE MM/dd/yyyy HH:mm z", Locale.US)
    val tFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm")

    def index = Action {
        Ok(views.html.chief("Playing with Chief."))
    }

    def graph = Action {
        implicit req =>
            val time = getCDTCurrentTime
            val pagingChief = formatString(uri, req.queryString)
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
                            else JsString(tFormat.format(tInFormat.parse(attributes(1).trim)))
                            v +(attributes(0).trim, jsValue)
                    }
            }

            val max = values.foldLeft((values.head \ "x").as[JsString]) { (max, jsObj) => if (tFormat.parse(max).getTime > tFormat.parse((jsObj \ "x").as[JsString]).getTime ) max else (jsObj \ "x").as[JsString] }
            Logger.debug("now => " + tFormat.format(time))
            Logger.debug("max => " + max)
            Result(
                header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
                body = Enumerator((values.size match {
                    case 0 => zeroValues(getActions(req), time)
                    case _ => Json.stringify(JsArray(values))
                }).getBytes)
            )
    }

    def zeroValues(actions: Seq[String], time: Long) = {
        actions.map { action =>
            formatString(dataTemplate, Map("action" -> Seq(action),
                "min" -> Seq(0.toString),
                "avg" -> Seq(0.toString),
                "max" -> Seq(0.toString),
                "count" -> Seq(0.toString),
                "time" -> Seq(tFormat.format(new Date(time)))))
        }.mkString(",")
    }

    def formatString(templateContent: String, params: Map[String, Seq[String]]) = {
        val template = new ST(templateContent)
        params.foreach {
            case (name: String, values: Seq[String]) => values.foreach(template.add(name, _))
        }
        template.render
    }

    def fake() = Action {
        implicit req =>
            val time = System.currentTimeMillis
            val actions = getActions(req)
            Result(
                header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
                body = Enumerator(("[" + Range(0, 6).reverse.foldLeft(List[String]()) { (coll, i) => coll :+ randomValues(actions, time, i)}.mkString(",") + "]").getBytes)
            )
    }

    def randomValues(actions: Seq[String], time: Long, factor: Long) = {
        actions.map { action =>
            val min: Int = Random.nextInt(10) + 1
            val max: Int = Random.nextInt(10) + 10
            formatString(dataTemplate, Map("action" -> Seq(action),
                "min" -> Seq(min.toString),
                "avg" -> Seq(((min + max) / 2).toString),
                "max" -> Seq(max.toString),
                "count" -> Seq((Random.nextInt(3) + 1).toString),
                "time" -> Seq(tFormat.format(new Date(time - 60000 * factor)))))
        }.mkString(",")
    }

    def getCDTCurrentTime = {
        Calendar.getInstance(TimeZone.getTimeZone("CDT")).getTime.getTime
    }

    def getActions(req:Request[AnyContent]) = {
        req.queryString("urls").map { url => url.substring(url.lastIndexOf("/")).trim}
    }

    implicit def Js2Scala(value:JsString) : String = value.value

}