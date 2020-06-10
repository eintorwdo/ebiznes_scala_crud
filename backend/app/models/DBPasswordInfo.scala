package models

import play.api.libs.json.Json

case class DBPasswordInfo(hasher: String, password: String, salt: Option[String], loginInfoId: String)

object DBPasswordInfo {
  implicit val passwordInfoFormat = Json.format[DBPasswordInfo]
}