package models

import play.api.libs.json._
import com.mohiva.play.silhouette.api.Identity

case class User(id: String, firstname: Option[String], lastname: Option[String], email: String, password: Option[String], role: String) extends Identity

object User {
  implicit val userFormat = Json.format[User]
}