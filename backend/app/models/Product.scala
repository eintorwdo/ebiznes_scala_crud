package models

import play.api.libs.json.Json

case class Product(id: Int, name: String, description: String, price: Int, amount: Int, manufacturer: Option[Int], category: Option[Int], subcategory: Option[Int])

object Product {
  implicit val productFormat = Json.format[Product]
}