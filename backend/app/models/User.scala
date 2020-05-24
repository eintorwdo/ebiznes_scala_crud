package models

import play.api.libs.json._

case class User(id: Int, name: String, email: String, password: String)

object User {
  implicit val userFormat = Json.format[User]
}