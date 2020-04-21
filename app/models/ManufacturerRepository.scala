package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManufacturerRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class ManufacturerTable(tag: Tag) extends Table[Manufacturer](tag, "manufacturer") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def * = (id, name) <> ((Manufacturer.apply _).tupled, Manufacturer.unapply)
  }

  val manufacturer = TableQuery[ManufacturerTable]

  def create(name: String): Future[Manufacturer] = db.run {
    (manufacturer.map(c => (c.name))
      returning manufacturer.map(_.id)
      into ((name, id) => Manufacturer(id, name))
      ) += (name)
  }

  def list(): Future[Seq[Manufacturer]] = db.run {
    manufacturer.result
  }

  def getById(id: Int): Future[Option[Manufacturer]] = db.run {
    manufacturer.filter(_.id === id).result.headOption
  }

  def update(id: Int, new_manufacturer: Manufacturer): Future[Unit] = {
    val manToUpdate: Manufacturer = new_manufacturer.copy(id)
    db.run(manufacturer.filter(_.id === id).update(manToUpdate)).map(_ => ())
  }
}