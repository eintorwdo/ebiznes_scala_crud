package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ Future, ExecutionContext }

@Singleton
class ProductRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val categoryRepository: CategoryRepository, val subCategoryRepository: SubCategoryRepository, val manufacturerRepository: ManufacturerRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._


  class ProductTable(tag: Tag) extends Table[Product](tag, "product") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def description = column[String]("description")

    def category = column[Option[Int]]("category")

    def price = column[Int]("price")

    def amount = column[Int]("amount")

    def manufacturer = column[Option[Int]]("manufacturer")

    def subcategory = column[Option[Int]]("subcategory")

    def subcategory_fk = foreignKey("subcat_fk", subcategory, subcat)(_.id?)

    def category_fk = foreignKey("cat_fk", category, subcat)(_.id?)

    def manufacturer_fk = foreignKey("man_fk", manufacturer, man)(_.id?)

    def * = (id, name, description, price, amount, manufacturer, category, subcategory) <> ((Product.apply _).tupled, Product.unapply)

  }

  /**
   * The starting point for all queries on the people table.
   */

  import categoryRepository.CategoryTable
  import subCategoryRepository.SubCategoryTable
  import manufacturerRepository.ManufacturerTable
  // import reviewRepository.ReviewTable

  private val product = TableQuery[ProductTable]

  private val cat = TableQuery[CategoryTable]

  private val subcat = TableQuery[SubCategoryTable]

  private val man = TableQuery[ManufacturerTable]

  // private val rev = TableQuery[ReviewTable]

  /**
   * Create a person with the given name and age.
   *
   * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
   * id for that person.
   */
  def create(name: String, description: String, price: Int, amount: Int, manufacturer: Option[Int], category: Option[Int], subcategory: Option[Int]): Future[Product] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (product.map(p => (p.name, p.description,p.price,p.amount,p.manufacturer,p.category,p.subcategory))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning product.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into {case ((name,description,price,amount,manufacturer,category,subcategory),id) => Product(id,name,description,price,amount,manufacturer,category,subcategory)}
      // And finally, insert the product into the database
      ) += (name,description,price,amount,manufacturer,category,subcategory)
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Product]] = db.run {
    product.result
  }

  def getByCategory(category_id: Int): Future[Seq[Product]] = db.run {
    product.filter(_.category === category_id).result
  }

  def getBySubCategory(subcategory_id: Int): Future[Seq[Product]] = db.run {
    product.filter(_.subcategory === subcategory_id).result
  }

  def getByCategories(category_ids: List[Int]): Future[Seq[Product]] = db.run {
    product.filter(_.category inSet category_ids).result
  }

  def delete(id: Int): Future[Unit] = db.run(product.filter(_.id === id).delete).map(_ => ())
    // val reviewQuery = rev.filter(_.product === id)
    // val orderDetailQuery = for{
    //   o <- ord if o.product === id
    // } yield o.product

    // val productQuery = product.filter(_.id === id)

    // db.run((reviewQuery.delete andThen productQuery.delete).transactionally).map(_ => ())

  def update(id: Int, new_product: Product): Future[Unit] = {
    val productToUpdate: Product = new_product.copy(id)
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
}