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
    def user = column[String]("user")
    def product = column[Int]("product")
    def date = column[String]("date")
    def userFk = foreignKey("usr_fk", user, usr)(_.id)
    def productFk = foreignKey("prd_fk", product, prd)(_.id)
    def * = (id, description, user, product, date) <> ((Review.apply _).tupled, Review.unapply)
  }

  import productRepository.ProductTable
  import userRepository.UserTable

  val review = TableQuery[ReviewTable]
  val prd = TableQuery[ProductTable]
  val usr = TableQuery[UserTable]

  def create(description: String, user: String, product: Int, date: String): Future[Review] = db.run {
    (review.map(c => (c.description,c.user,c.product,c.date))
      returning review.map(_.id)
      into {case ((description, user, product, date), id) => Review(id, description, user, product, date)}
      ) += (description, user, product, date)
  }

  def listByProdId(id: Int): Future[Seq[(Review, User)]] = db.run {
    (for {
      (review, user) <- review.filter(_.product === id) join usr on (_.user === _.id)
    } yield (review, user)).result
  }

  def list(): Future[Seq[Review]] = db.run {
    review.result
  }

  def getById(id: Int): Future[Option[Review]] = db.run {
    review.filter(_.id === id).result.headOption
  }

  def update(id: Int, newReview: Review): Future[Unit] = {
    val revToUpdate: Review = newReview.copy(id)
    db.run(review.filter(_.id === id).update(revToUpdate)).map(_ => ())
  }

  def delete(id: Int): Future[Unit] = db.run(review.filter(_.id === id).delete).map(_ => ())

  def deleteByProductId(id: Int): Future[Unit] = db.run(review.filter(_.product === id).delete).map(_ => ())

  def deleteByUserId(id: String): Future[Unit] = db.run(review.filter(_.user === id).delete).map(_ => ())
}