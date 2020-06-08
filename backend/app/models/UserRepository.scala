package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
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

  def update(id: String, new_user: User): Future[Unit] = {
    val userToUpdate: User = new_user.copy(id)
    db.run(user.filter(_.id === id).update(userToUpdate)).map(_ => ())
  }

  def delete(id: String): Future[Unit] = db.run(user.filter(_.id === id).delete).map(_ => ())
}