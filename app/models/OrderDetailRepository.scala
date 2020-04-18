package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrderDetailRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, orderRepository: OrderRepository, productRepository: ProductRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class OrderDetailTable(tag: Tag) extends Table[OrderDetail](tag, "orderdetail") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def price = column[Int]("price")
    def order = column[Int]("order_")
    def product = column[Int]("product")
    def order_fk = foreignKey("order_fk", order, ord)(_.id)
    def product_fk = foreignKey("prd_fk", product, prd)(_.id)
    def * = (id, price, order, product) <> ((OrderDetail.apply _).tupled, OrderDetail.unapply)
  }

  import orderRepository.OrderTable
  import productRepository.ProductTable

  private val orderdetail = TableQuery[OrderDetailTable]
  private val ord = TableQuery[OrderTable]
  private val prd = TableQuery[ProductTable]

  def create(price: Int, order: Int, product: Int): Future[OrderDetail] = db.run {
    (orderdetail.map(c => (c.price,c.order,c.product))
      returning orderdetail.map(_.id)
      into {case((price, order, product), id) => OrderDetail(id, price, order, product)}
      ) += (price, order, product)
  }

  def list(): Future[Seq[OrderDetail]] = db.run {
    orderdetail.result
  }

  def getByOrderId(id: Int): Future[Seq[OrderDetail]] = db.run {
    orderdetail.filter(_.order === id).result
  }
}