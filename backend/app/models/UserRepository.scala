package models

import com.mohiva.play.silhouette.api.{LoginInfo}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends AuthTables {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "user") {
    def id = column[String]("id", O.PrimaryKey)
    def firstname = column[Option[String]]("firstname")
    def lastname = column[Option[String]]("lastname")
    def email = column[String]("email")
    def password = column[Option[String]]("password")
    def role = column[String]("role")
    def * = (id, firstname, lastname, email, password, role) <> ((User.apply _).tupled, User.unapply)
  }

  val user = TableQuery[UserTable]

  def create(firstname: Option[String], lastname: Option[String], email: String, password: Option[String], role: String): Future[Int] = {
    val id = UUID.randomUUID().toString()
    db.run{
      user += User(id, firstname, lastname, email, password, role)
    }
  }

  def list(): Future[Seq[User]] = db.run {
    user.result
  }

  def getById(id: String): Future[Option[User]] = db.run {
    user.filter(_.id === id).result.headOption
  }

  def getByEmail(email: String): Future[Option[User]] = db.run {
    user.filter(_.email === email).result.headOption
  }

  def update(id: String, new_user: User): Future[Unit] = {
    val userToUpdate: User = new_user.copy(id)
    db.run(user.filter(_.id === id).update(userToUpdate)).map(_ => ())
  }

  def delete(id: String): Future[Unit] = db.run(user.filter(_.id === id).delete).map(_ => ())

  def save(userToSave: User) = db.run {
    user.insertOrUpdate(userToSave).map(_ => userToSave)
  }

  def updateNoId(updatedUser: User) = db.run {
    user.filter(_.id === updatedUser.id).update(updatedUser).map(_ => updatedUser)
  }

  def find(loginInfo: LoginInfo) = {
    val findLoginInfoQuery = loginInfos.filter(dbLoginInfo =>
      dbLoginInfo.providerID === loginInfo.providerID &&  dbLoginInfo.providerKey === loginInfo.providerKey)
    val query = for {
      dbLoginInfo <- findLoginInfoQuery
      dbUserLoginInfo <- userLoginInfos.filter(_.loginInfoId === dbLoginInfo.id)
      dbUser <- user.filter(_.id === dbUserLoginInfo.userID)
    } yield dbUser
    db.run(query.result.headOption).map { userOption =>
      userOption.map { user =>
        User(user.id, user.firstname, user.lastname, user.email, user.password, user.role)
      }
    }
  }
}