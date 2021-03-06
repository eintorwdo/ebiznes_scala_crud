package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ Future, ExecutionContext }
import org.sqlite.core.DB
import slick.sql.SqlAction

@Singleton
class ProductRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val categoryRepository: CategoryRepository, val subCategoryRepository: SubCategoryRepository, val manufacturerRepository: ManufacturerRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._


  class ProductTable(tag: Tag) extends Table[Product](tag, "product") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def description = column[String]("description")

    def category = column[Option[Int]]("category")

    def price = column[Int]("price")

    def amount = column[Int]("amount")

    def manufacturer = column[Option[Int]]("manufacturer")

    def subcategory = column[Option[Int]]("subcategory")

    def subcategoryFk = foreignKey("subcat_fk", subcategory, subcat)(_.id?)

    def categoryFk = foreignKey("cat_fk", category, subcat)(_.id?)

    def manufacturerFk = foreignKey("man_fk", manufacturer, man)(_.id?)

    def * = (id, name, description, price, amount, manufacturer, category, subcategory) <> ((Product.apply _).tupled, Product.unapply)

  }

  import categoryRepository.CategoryTable
  import subCategoryRepository.SubCategoryTable
  import manufacturerRepository.ManufacturerTable

  private val product = TableQuery[ProductTable]

  private val cat = TableQuery[CategoryTable]

  private val subcat = TableQuery[SubCategoryTable]

  private val man = TableQuery[ManufacturerTable]

  def create(name: String, description: String, price: Int, amount: Int, manufacturer: Option[Int], category: Option[Int], subcategory: Option[Int]): Future[Product] = db.run {
    (product.map(p => (p.name, p.description,p.price,p.amount,p.manufacturer,p.category,p.subcategory))
      returning product.map(_.id)
      into {case ((name,description,price,amount,manufacturer,category,subcategory),id) => Product(id,name,description,price,amount,manufacturer,category,subcategory)}
      ) += (name,description,price,amount,manufacturer,category,subcategory)
  }

  def list(): Future[Seq[Product]] = db.run {
    product.result
  }

  def search(query: String): Future[Seq[Product]] = {
    val src = for {
      p <- product if p.name like s"%${query}%"
    } yield (p)
    db.run {
      src.result
    }
  }

  def searchInCategory(query: String, category: Int): Future[Seq[Product]] = {
    val src = for {
      p <- product if (p.name like s"%${query}%") && p.category.getOrElse(0) === category
    } yield (p)
    db.run {
      src.result
    }
  }

  def searchInSubCategory(query: String, subcategory: Int): Future[Seq[Product]] = {
    val src = for {
      p <- product if (p.name like s"%${query}%") && (p.subcategory.getOrElse(0) === subcategory)
    } yield (p)
    db.run {
      src.result
    }
  }

  def getByCategory(categoryId: Int): Future[Seq[Product]] = db.run {
    product.filter(_.category === categoryId).result
  }

  def getBySubCategory(subcategoryId: Int): Future[Seq[Product]] = db.run {
    product.filter(_.subcategory === subcategoryId).result
  }

  def getByCategories(categoryIds: List[Int]): Future[Seq[Product]] = db.run {
    product.filter(_.category inSet categoryIds).result
  }

  def delete(id: Int): Future[Unit] = db.run(product.filter(_.id === id).delete).map(_ => ())

  def update(id: Int, newProduct: Product): Future[Unit] = {
    val productToUpdate: Product = newProduct.copy(id)
    db.run(product.filter(_.id === id).update(productToUpdate)).map(_ => ())
  }

  def getById(id: Int): Future[Seq[(Product, Option[Manufacturer], Option[Category], Option[SubCategory])]] = db.run {
    (for {
      (((product, manufacturer), category), subcategory) <- product.filter(_.id === id) joinLeft man on (_.manufacturer === _.id) joinLeft cat on (_._1.category === _.id) joinLeft subcat on (_._1._1.subcategory === _.id)
    } yield (product, manufacturer, category, subcategory)).result
  }

  def deleteManufacturerId(id: Int): Future[Unit] = {
    val manDetailQuery = for{
      p <- product if p.manufacturer === id
    } yield p.manufacturer

     db.run(manDetailQuery.update(None)).map(_ => ())
  }

  def deleteCategoryId(id: Int): Future[Unit] = {
    val catQuery = for{
      p <- product if p.category === id
    } yield p.category

     db.run(catQuery.update(None)).map(_ => ())
  }

  def deleteSubCategoryId(id: Int): Future[Unit] = {
    val subCatQuery = for{
      p <- product if p.subcategory === id
    } yield p.subcategory

     db.run(subCatQuery.update(None)).map(_ => ())
  }

  def getRandomProducts(category: Int): Future[Seq[(Int, String, String, Int, Int, Option[Int], Option[Int], Option[Int])]] = db.run{
    sql"SELECT * FROM product WHERE category=$category ORDER BY RANDOM() LIMIT 3".as[(Int, String, String, Int, Int, Option[Int], Option[Int], Option[Int])]
  }

  def productsWithIds(ids: List[Int]): Future[Seq[Product]] = {
    val query = product.filter(_.id.inSet(ids)).result
    db.run(query)
  }

  def updateProductsAfterOrder(prds: Seq[(Int, Int)]) = {
    val NUM = prds.length
    val ids = prds.map(p => p._1)

    val updates = prds.map(p => {
      sqlu"""UPDATE product SET amount = amount - ${p._2} WHERE id = ${p._1}"""
    })

    println(updates.mkString(";"))

    val action = product.filter(x => (x.id.inSet(ids))).result.map(_.filter(y => y.amount - prds.filter(prd => prd._1 == y.id).head._2 >= 0)).map(p => p.length).flatMap{
      case NUM => {
        DBIO.sequence(updates)
      }
      case _ => {DBIO.sequence(Seq())}
    }.transactionally

    db.run(action)
  }
}