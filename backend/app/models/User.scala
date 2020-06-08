package models

import play.api.libs.json._

case class User(id: String, firstname: Option[String], lastname: Option[String], email: String, password: Option[String], role: String)

object User {
  implicit val userFormat = Json.format[User]
}