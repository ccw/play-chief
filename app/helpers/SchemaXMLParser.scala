package helpers

import org.xml.sax.InputSource
import scales.utils._
import scales.xml._
import ScalesXml._
import Functions._
import play.api.libs.json.{JsBoolean, JsString, Json, JsArray}

/**
 * SchemaXMLParser 
 *
 * @author <a href="mailto:kchen@digitalriver.com">Ken Chen</a>
 */
object SchemaXMLParser {

  def parse(source: InputSource):JsArray = {
    val path = top(loadXml(source))
    (path \\* "TABLE").foldLeft(JsArray()) {
      (tables, table) =>
        val tableName = string(table \* "NAME").trim
        val indices = path \* (child => name(child).qName == "INDEXES_FOR_TABLE" && string(child \* "NAME").trim == tableName)
        tables :+ Json.obj(
          "name" -> JsString(tableName),
          "columns" -> (table \\* (column => name(column).qName == "COLUMN" && name((column \^) \^).qName == "TABLE")).foldLeft(JsArray())((t, c) => {
            val colName: String = string(c \* "NAME").trim
            t :+ Json.obj(
              "name" -> JsString(colName),
              "type" -> JsString(string(c \* "DATA_TYPE").trim),
              "nullable" -> JsBoolean(isNullable(table, colName)),
              "primaryKey" -> JsBoolean(isPrimaryKey(table, colName))
            )
          }
          ),
          "indices" -> (indices \\* "INDEX").foldLeft(JsArray())((t, c) =>
            t :+ Json.obj(
              "name" -> JsString(string(c \* "NAME").trim),
              "columns" -> (c \\* "COLUMN").foldLeft(JsArray())((a, b) =>
                a :+ JsString(string(b \* "NAME").trim)
              )
            )
          )
        )
    }
  }

  def isNullable(table: XmlPath, colName: String): Boolean = {
    (table \\* (constraint => name(constraint).qName == "CONSTRAINT" &&
      string(constraint \* "TYPE").trim == "CHECK" &&
      string(constraint \* "EXPRESSION").trim == "\"" + colName + "\" IS NOT NULL")).size == 0
  }

  def isPrimaryKey(table: XmlPath, colName: String): Boolean = {
    (table \\* (constraint => name(constraint).qName == "CONSTRAINT" &&
      string(constraint \* "TYPE").trim == "PRIMARY KEY" &&
      string(constraint \\* (column => name(column).qName == "COLUMN" && string(column \* "NAME").trim == colName)))).size > 0
  }

}
