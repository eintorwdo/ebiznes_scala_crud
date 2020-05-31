package models

import play.api.libs.json._

case class OrderDetail(id: Int, price: Int, order: Int, product: Option[Int], amount: Int)

object OrderDetail {
  implicit val orderDetailFormat = Json.format[OrderDetail]
}