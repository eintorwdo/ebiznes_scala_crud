package models

import play.api.libs.json.Json

case class Order(id: Int, price: Int, date: String, address: String, sent: Int, user: Int, payment: Int, delivery: Int)

object Order {
  implicit val orderFormat = Json.format[Order]
}