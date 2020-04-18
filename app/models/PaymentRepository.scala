package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class PaymentTable(tag: Tag) extends Table[Payment](tag, "payment") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def * = (id, name) <> ((Payment.apply _).tupled, Payment.unapply)
  }

  val payment = TableQuery[PaymentTable]

  def create(name: String): Future[Payment] = db.run {
    (payment.map(c => (c.name))
      returning payment.map(_.id)
      into ((name, id) => Payment(id, name))
      ) += (name)
  }

  def list(): Future[Seq[Payment]] = db.run {
    payment.result
  }

  def getById(id: Int): Future[Payment] = db.run {
    payment.filter(_.id === id).result.head
  }
}