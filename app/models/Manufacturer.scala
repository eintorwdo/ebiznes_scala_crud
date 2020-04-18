package models

import play.api.libs.json._

case class Manufacturer(id: Int, name: String)

object Manufacturer {
  implicit val manufacturerFormat = Json.format[Manufacturer]
}