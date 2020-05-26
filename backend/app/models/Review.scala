package models

import play.api.libs.json._

case class Review(id: Int, description: String, user: Int, product: Int, date: String)

object Review {
  implicit val reviewFormat = Json.format[Review]
}