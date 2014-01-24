package controllers

import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import dispatch._
import scala._
import org.stringtemplate.v4.ST
import java.text.SimpleDateFormat
import play.api.libs.concurrent.Execution.Implicits._
import play.Logger
import java.util.Date
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
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

  def commits(owner:String, repo:String) = Action {
    implicit req =>
      val target = getUrlFrom(Map("owner" -> owner, "repo" -> repo, "since" -> formatDate(req.queryString("since") match {
        case date:Seq[String] => date.mkString
        case _ => new SimpleDateFormat(inFormat).format(new Date())
      })))
      Logger.debug("To invoke [" + target + "]")
      val commits = query(target).as[JsArray].value.map( commit =>
        Json.obj(
            "meta" -> Json.obj(
              "sha" -> (commit \ "sha"),
              "committer" -> (commit \ "commit" \ "committer" \ "name"),
              "commit_date" -> (commit \ "commit" \ "committer" \ "date")
            ),
            "url" -> JsString((commit \ "commit" \ "tree" \ "url").as[JsString].value + "?recursive=1")
        )
      )
    Ok.chunked(fetchTreeContents(commits)).as("application/json")
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

  def fetchTreeContents(commits:Seq[JsObject]) = {
    Enumerator(commits: _*).map(
      commit => query((commit \ "url").as[JsString].value).as[JsObject] +("meta", commit \ "meta")).map(
        commit => {
          Json.obj(
            "meta" -> commit \ "meta",
            "files" -> JsArray((commit \ "tree" \\ "path").map(path => {
              val strPath = path.as[JsString].value
              val folder = strPath.lastIndexOf("/") match {
                case n:Int if n >= 0 => strPath.substring(0, n + 1)
                case _ => "/"
              }
              val file = folder match {
                case "/" => strPath
                case _ => strPath.replace(folder, "")
              }
              Json.obj(
                "full_path" -> path,
                "path" -> JsString(folder),
                "file" -> JsString(file)
              )
            }))
          )
        })
  }

}
