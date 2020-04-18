package models

import play.api.libs.json._

case class OrderDetail(id: Int, price: Int, order: Int, product: Int)

object OrderDetail {
  implicit val orderDetailFormat = Json.format[OrderDetail]
}