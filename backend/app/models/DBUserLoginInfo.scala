package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{Tag}

case class DBUserLoginInfo(
    userID: String,
    loginInfoId: String
  )

class UserLoginInfoTable(tag: Tag) extends Table[DBUserLoginInfo](tag, "user_login_info") {
  def userID = column[String]("user_id")
  def loginInfoId = column[String]("login_info_id")
  def * = (userID, loginInfoId) <> (DBUserLoginInfo.tupled, DBUserLoginInfo.unapply)
}