package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeliveryRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class DeliveryTable(tag: Tag) extends Table[Delivery](tag, "delivery") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def price = column[Int]("price")
    def * = (id, name, price) <> ((Delivery.apply _).tupled, Delivery.unapply)
  }

  val delivery = TableQuery[DeliveryTable]

  def create(name: String, price: Int): Future[Delivery] = db.run {
    (delivery.map(c => (c.name, c.price))
      returning delivery.map(_.id)
      into {case((name, price), id) => Delivery(id, name, price)}
      ) += (name, price)
  }

  def list(): Future[Seq[Delivery]] = db.run {
    delivery.result
  }

  def getById(id: Int): Future[Option[Delivery]] = db.run {
    delivery.filter(_.id === id).result.headOption
  }

  def update(id: Int, new_delivery: Delivery): Future[Unit] = {
    val deliveryToUpdate: Delivery = new_delivery.copy(id)
    db.run(delivery.filter(_.id === id).update(deliveryToUpdate)).map(_ => ())
  }

  def delete(id: Int): Future[Unit] = db.run(delivery.filter(_.id === id).delete).map(_ => ())
}