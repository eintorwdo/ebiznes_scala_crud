package models
import com.mohiva.play.silhouette.api.{LoginInfo}

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{Tag}

case class DBLoginInfo(id: String, providerID: String, providerKey: String)

class LoginInfoTable(tag: Tag) extends Table[DBLoginInfo](tag, "login_info") {
    def id = column[String]("id", O.PrimaryKey)
    def providerID = column[String]("provider_id")
    def providerKey = column[String]("provider_key")
    def * = (id, providerID, providerKey) <> ((DBLoginInfo.apply _).tupled, DBLoginInfo.unapply)
}