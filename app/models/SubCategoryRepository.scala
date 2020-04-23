package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubCategoryRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val categoryRepository: CategoryRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class SubCategoryTable(tag: Tag) extends Table[SubCategory](tag, "subcategory") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def category = column[Int]("category")
    def category_fk = foreignKey("cat_fk", category, subcategory)(_.id)
    def * = (id, name, category) <> ((SubCategory.apply _).tupled, SubCategory.unapply)
  }

  import categoryRepository.CategoryTable

  val subcategory = TableQuery[SubCategoryTable]
  val category = TableQuery[CategoryTable]

  def create(name: String, category: Int): Future[SubCategory] = db.run {
    (subcategory.map(c => (c.name,c.category))
      returning subcategory.map(_.id)
      into {case ((name, category), id) => SubCategory(id, name, category)}
      ) += (name, category)
  }

  def list(): Future[Seq[SubCategory]] = db.run {
    subcategory.result
  }

  def getById(id: Int): Future[Option[SubCategory]] = db.run {
    subcategory.filter(_.id === id).result.headOption
  }

  def update(id: Int, new_subcategory: SubCategory): Future[Unit] = {
    val subCategoryToUpdate: SubCategory = new_subcategory.copy(id)
    db.run(subcategory.filter(_.id === id).update(subCategoryToUpdate)).map(_ => ())
  }

  def delete(id: Int): Future[Unit] = db.run(subcategory.filter(_.id === id).delete).map(_ => ())

  def deleteByCategoryId(id: Int): Future[Unit] = db.run(subcategory.filter(_.category === id).delete).map(_ => ())
}