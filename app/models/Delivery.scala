package models

import play.api.libs.json._

case class Delivery(id: Int, name: String, price: Int)

object Delivery {
  implicit val deliveryFormat = Json.format[Delivery]
}