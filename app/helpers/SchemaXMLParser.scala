package helpers

import org.xml.sax.InputSource
import scales.utils._
import scales.xml._
import ScalesXml._
import play.api.libs.json.{JsBoolean, JsString, Json, JsArray}
import scala.xml.{Node, XML}

trait XMLParser {
  def parse(source: InputSource): JsArray
}

object ScalesSchemaXMLParser extends XMLParser {

  def parse(source: InputSource): JsArray = {
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
          }),
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

object StandardSchemaXMLParser extends XMLParser {

  def parse(source: InputSource) = {
    val root = XML.load(source)
    val tables = root.child filter (node => node.label == "TABLE")
    val indices = root.child filter (node => node.label == "INDEXES_FOR_TABLE")
    tables.foldLeft(JsArray()) {
      (jsTables, xmlTableNode) =>
        val tableName = (xmlTableNode \ "NAME").text.trim
        jsTables :+ Json.obj(
          "name" -> JsString(tableName),
          "columns" -> (xmlTableNode \ "COLUMNS" \ "COLUMN").foldLeft(JsArray())((jsColumns, xmlColumn) => {
            val colName = (xmlColumn \ "NAME").text.trim
            jsColumns :+ Json.obj(
              "name" -> JsString(colName),
              "type" -> JsString((xmlColumn \ "DATA_TYPE").text.trim),
              "nullable" -> JsBoolean(isNullable(xmlTableNode, colName)),
              "primaryKey" -> JsBoolean(isPrimaryKey(xmlTableNode, colName))
            )
          }),
          "indices" -> indices.filter(idx => (idx \ "NAME").text.trim == tableName).flatMap(idx => idx \\ "INDEX").foldLeft(JsArray())((jsIndices, xmlIndex) =>
            jsIndices :+ Json.obj(
              "name" -> JsString((xmlIndex \ "NAME").text.trim),
              "columns" -> (xmlIndex \\ "COLUMN").foldLeft(JsArray())((jsColumns, xmlColumn) =>
                jsColumns :+ JsString((xmlColumn \ "NAME").text.trim)
              )
            )
          )
        )
    }
  }

  def isNullable(table: Node, colName: String): Boolean = {
    (table \\ "CONSTRAINT" \\ "EXPRESSION").filter(constraint => constraint.text.trim == "\"" + colName + "\" IS NOT NULL").length == 0
  }

  def isPrimaryKey(table: Node, colName: String): Boolean = {
    (table \\ "CONSTRAINT").filter(constraint => (constraint \\ "TYPE").text.trim == "PRIMARY KEY")
      .filter(constraint => (constraint \\ "COLUMN").filter(column => (column \\ "NAME").text.trim == colName)).length > 0
  }

}

trait SchemaXMLParser {

  def schemaParser: XMLParser = {
    StandardSchemaXMLParser
  }

  def parse(source: Option[InputSource]): JsArray = {
    source match {
      case Some(input) => schemaParser.parse(input)
      case _ => JsArray()
    }
  }

}

/**
 * SchemaXMLParser
 *
 * @author <a href="mailto:kchen@digitalriver.com">Ken Chen</a>
 */
object SchemaXMLParser extends SchemaXMLParser {

  def withImpl(impl: XMLParser) = {
    new SchemaXMLParser {
      override val schemaParser = impl
    }
  }

}
