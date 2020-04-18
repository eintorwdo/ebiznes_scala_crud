package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReviewRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val productRepository: ProductRepository, val userRepository: UserRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class ReviewTable(tag: Tag) extends Table[Review](tag, "review") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def description = column[String]("description")
    def user = column[Int]("user")
    def product = column[Int]("product")
    def user_fk = foreignKey("usr_fk", user, usr)(_.id)
    def product_fk = foreignKey("prd_fk", product, prd)(_.id)
    def * = (id, description, user, product) <> ((Review.apply _).tupled, Review.unapply)
  }

  import productRepository.ProductTable
  import userRepository.UserTable

  val review = TableQuery[ReviewTable]
  val prd = TableQuery[ProductTable]
  val usr = TableQuery[UserTable]

  def create(description: String, user: Int, product: Int): Future[Review] = db.run {
    (review.map(c => (c.description,c.user,c.product))
      returning review.map(_.id)
      into {case ((description, user, product), id) => Review(id, description, user, product)}
      ) += (description, user, product)
  }

  def list(): Future[Seq[Review]] = db.run {
    review.result
  }

  def getById(id: Int): Future[Review] = db.run {
    review.filter(_.id === id).result.head
  }
}