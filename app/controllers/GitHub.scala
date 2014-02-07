package controllers

import play.api.mvc._
import scala._
import org.stringtemplate.v4.ST
import java.text.SimpleDateFormat
import play.Logger
import java.util.Date
import helpers.GitHubJSONParser
import play.api.libs.json.JsObject

/**
 * GitHub 
 *
 * @author <a href="mailto:kchen@digitalriver.com">Ken Chen</a>
 */
object GitHub extends Controller {

  val apiURLTemplate = "http://github.digitalriverws.net/api/v3/repos/<owner>/<repo>/commits?since=<since>"
  val inFormat = "yyyyMMdd"
  val outFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  def index = Action {
    Ok(views.html.github("GitHub Monitor"))
  }

  def commits(owner: String, repo: String) = Action {
    implicit req =>
      val target = getUrlFrom(Map("owner" -> owner, "repo" -> repo, "since" -> formatDate(req.queryString("since") match {
        case date: Seq[String] => date.mkString
        case _ => new SimpleDateFormat(inFormat).format(new Date())
      })))
      Logger.debug("To invoke [" + target + "]")
      Ok.chunked(GitHubJSONParser.parse(target)).as("application/json")
  }

  def formatDate(date: String) = {
    val ifo = new SimpleDateFormat(inFormat)
    val ofo = new SimpleDateFormat(outFormat)
    ofo.format(ifo.parse(date))
  }

  def getUrlFrom(params: Map[String, String]) = {
    val template = new ST(apiURLTemplate)
    params.foreach(p => template.add(p._1, p._2))
    template.render
  }


}
