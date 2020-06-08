package models

import play.api.libs.json._

case class DBOAuth2Info(
    id: String,
    accessToken: String,
    tokenType: Option[String],
    expiresIn: Option[Int],
    refreshToken: Option[String],
    loginInfoId: String
)

object DBOAuth2Info {
  implicit val oauth2InfoFormat = Json.format[DBOAuth2Info]
}