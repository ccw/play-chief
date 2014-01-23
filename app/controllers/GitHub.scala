package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import org.jsoup.Jsoup
import dispatch._
import org.jsoup.nodes.Element
import org.jsoup.helper.StringUtil
import scala._
import play.api.libs.json.JsArray
import play.api.mvc.ResponseHeader
import play.api.mvc.SimpleResult
import play.api.libs.json.JsNumber
import org.stringtemplate.v4.ST
import java.text.SimpleDateFormat
import play.api.libs.concurrent.Execution.Implicits._
import play.Logger
import rx.lang.scala.{Subscription, Observer, Observable}

/**
 * GitHub 
 *
 * @author <a href="mailto:kchen@digitalriver.com">Ken Chen</a>
 */
object GitHub extends Controller {

  val apiURLTemplate = "http://github.digitalriverws.net/api/v3/repos/GlobalCommerce/pacific-<repo>/commits?since=<since>"
  val inFormat = "yyyy/MM/dd"
  val outFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  def commits(repo:String) = Action {
    implicit req =>
      val target = getUrlFrom(Map("repo" -> repo, "since" -> formatDate(req.queryString("since").mkString)))
      Logger.debug("To invoke [" + target + "]")
      val values = query(target).as[JsArray].value.map( commit =>
        Json.obj(
            "commit" -> Json.obj(
              "sha" -> (commit \ "sha"),
              "committer" -> (commit \ "commit" \ "committer" \ "name"),
              "commit_date" -> (commit \ "commit" \ "committer" \ "date")
            ),
            "url" -> JsString((commit \ "commit" \ "tree" \ "url").as[JsString].value + "?recursive=1")
        )
      )
      fetchTreeContents(values)
      SimpleResult(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
        body = Enumerator(Json.stringify(JsArray(values)).getBytes)
      )
  }

  def query(URL:String) = {
    Json.parse(Http(url(URL) OK as.String).apply())
  }

  def formatDate(date:String) = {
    val ifo = new SimpleDateFormat(inFormat)
    val ofo = new SimpleDateFormat(outFormat)
    ofo.format(ifo.parse(date))
  }

  def getUrlFrom(params: Map[String, String]) = {
    val template = new ST(apiURLTemplate)
    params.foreach(p => template.add(p._1, p._2))
    template.render
  }

  def fetchTreeContents(trees:Seq[JsObject]) = {
      Observable(trees:_*)
        .map(tree => query((tree \ "url").as[JsString].value).as[JsObject] + ("commit", tree \ "commit"))
        .filter(jsTrees => (jsTrees \ "tree" \\ "path")
                .filter( _.as[JsString].value.indexOf("catalog") > -1 ).nonEmpty )
        .subscribe(tree => Logger.debug("Found files [" + (tree \ "tree" \\ "path").mkString(",") + "] @ commit -> " + Json.stringify(tree \ "commit")))
  }

}
