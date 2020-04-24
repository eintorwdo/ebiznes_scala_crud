package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "user") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def password = column[String]("password")
    def * = (id, name, email, password) <> ((User.apply _).tupled, User.unapply)
  }

  val user = TableQuery[UserTable]

  def create(name: String, email: String, password: String): Future[User] = db.run {
    (user.map(c => (c.name, c.email, c.password))
      returning user.map(_.id)
      into {case((name,email,password), id) => User(id, name, email, password)}
      ) += (name, email, password)
  }

  def list(): Future[Seq[User]] = db.run {
    user.result
  }

  def getById(id: Int): Future[Option[User]] = db.run {
    user.filter(_.id === id).result.headOption
  }

  def update(id: Int, new_user: User): Future[Unit] = {
    val userToUpdate: User = new_user.copy(id)
    db.run(user.filter(_.id === id).update(userToUpdate)).map(_ => ())
  }

  def delete(id: Int): Future[Unit] = db.run(user.filter(_.id === id).delete).map(_ => ())
}