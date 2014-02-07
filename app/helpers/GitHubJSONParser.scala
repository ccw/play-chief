package helpers

import play.api.libs.iteratee.{Enumeratee, Enumerator}
import play.api.libs.json._
import dispatch._
import scala._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.Logger

/**
 * GitHubJSONParser 
 *
 * @author <a href="mailto:kchen@digitalriver.com">Ken Chen</a>
 */
object GitHubJSONParser {

  def parse(target:String) = {
    val meta = query(target).as[JsArray].value.map( metaData =>
      Json.obj(
        "meta" -> Json.obj(
          "sha" -> (metaData \ "sha"),
          "committer" -> (metaData \ "commit" \ "committer" \ "name"),
          "commit_date" -> (metaData \ "commit" \ "committer" \ "date")
        ),
        "url" -> JsString((metaData \ "commit" \ "tree" \ "url").as[JsString].value + "?recursive=1")
      )
    )
    parseAndStreamCommitTree(meta)
  }
  
  def parseAndStreamCommitTree(metaData:Seq[JsObject]) = {
    (Enumerator(metaData: _*) &> fetchCommitTree) >>> Enumerator.eof
  }

  def fetchCommitTree = Enumeratee.map[JsObject] { metaData =>
    Logger.info("... fetching commit [" + (metaData \ "meta"  \ "sha").as[JsString].value + "]")
    val commit = query((metaData \ "url").as[JsString].value).as[JsObject] + ("meta", metaData \ "meta")
    Json.obj(
      "meta" -> commit \ "meta",
      "files" -> JsArray((commit \ "tree").as[JsArray].value.filter(elem => "blob" == (elem \ "type").as[JsString].value).map( elem => {
        val path: JsValue = elem \ "path"
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
  }

  private def query(URL:String) = {
    Logger.info("...... querying [" + URL + "]")
    Json.parse(Http(url(URL) OK as.String).apply())
  }
  
}
