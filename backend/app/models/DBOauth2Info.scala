package models

import play.api.libs.json._
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{Tag}

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

class OAuth2InfoTable(tag: Tag) extends Table[DBOAuth2Info](tag, "oauth2_info") {
    def id = column[String]("id", O.PrimaryKey)
    def accessToken = column[String]("access_token")
    def tokenType = column[Option[String]]("token_type")
    def expiresIn = column[Option[Int]]("expires_in")
    def refreshToken = column[Option[String]]("refresh_token")
    def loginInfoId = column[String]("login_info_id")
    def * = (id, accessToken, tokenType, expiresIn, refreshToken, loginInfoId) <> ((DBOAuth2Info.apply _).tupled, DBOAuth2Info.unapply)
  }