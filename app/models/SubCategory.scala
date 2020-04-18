package models

import play.api.libs.json._

case class SubCategory(id: Int, name: String, category: Int)

object SubCategory {
  implicit val subcategoryFormat = Json.format[SubCategory]
}