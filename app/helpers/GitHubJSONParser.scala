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

  def parse(commitURI:String, branchURI:String):Enumerator[JsObject] = {
    ((Enumerator((branchURI, commitURI)) &> toJSONBranches).flatMap(branches => Enumerator(branches.map(fetchCommit).flatten: _*)) &> toJSONCommits) >>> Enumerator.eof
  }

  def toJSONBranches = Enumeratee.map[(String, String)] { uris =>
    query(uris._1).as[JsArray].value.map( branch =>
      Json.obj(
        "name" -> branch \ "name",
        "url" -> JsString(uris._2 + "&sha=" + (branch \ "name").as[JsString].value)
      ))
  }

  def fetchCommit(branch:JsObject):Seq[JsObject] = {
    Logger.debug("... fetching commit for branch [" + (branch \ "name").as[JsString].value + "]")
    query((branch \ "url").as[JsString].value).as[JsArray].value.map( metaData =>
      Json.obj(
        "meta" -> Json.obj(
          "branch" -> (branch \ "name"),
          "sha" -> (metaData \ "sha"),
          "message" -> (metaData \ "commit" \ "message"),
          "committer" -> (metaData \ "commit" \ "committer" \ "name"),
          "commit_date" -> (metaData \ "commit" \ "committer" \ "date")
        ),
        "url" -> (metaData \ "url")
      )
    )
  }

  def toJSONCommits = Enumeratee.map[JsObject] { metaData =>
    Logger.debug("... fetching tree for commit [" + (metaData \ "meta"  \ "sha").as[JsString].value + "]")
    Json.obj(
      "meta" -> metaData \ "meta",
      "files" -> JsArray((query((metaData \ "url").as[JsString].value).as[JsObject] \ "files").as[JsArray].value.map( elem => {
        val path: JsValue = elem \ "filename"
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
    Logger.debug("...... querying [" + URL + "]")
    Json.parse(Http(url(URL) OK as.String).apply())
  }
  
}
