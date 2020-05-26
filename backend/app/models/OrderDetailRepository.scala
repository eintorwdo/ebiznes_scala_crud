package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrderDetailRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val orderRepository: OrderRepository, val productRepository: ProductRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class OrderDetailTable(tag: Tag) extends Table[OrderDetail](tag, "orderdetail") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def price = column[Int]("price")
    def order = column[Int]("order_")
    def product = column[Option[Int]]("product")
    def order_fk = foreignKey("order_fk", order, ord)(_.id)
    def product_fk = foreignKey("prd_fk", product, prd)(_.id?)
    def * = (id, price, order, product) <> ((OrderDetail.apply _).tupled, OrderDetail.unapply)
  }

  import orderRepository.OrderTable
  import productRepository.ProductTable

  private val orderdetail = TableQuery[OrderDetailTable]
  private val ord = TableQuery[OrderTable]
  private val prd = TableQuery[ProductTable]

  def create(price: Int, order: Int, product: Option[Int]): Future[OrderDetail] = db.run {
    (orderdetail.map(c => (c.price,c.order,c.product))
      returning orderdetail.map(_.id)
      into {case((price, order, product), id) => OrderDetail(id, price, order, product)}
      ) += (price, order, product)
  }

  def list(): Future[Seq[OrderDetail]] = db.run {
    orderdetail.result
  }

  def getByOrderId(id: Int): Future[Seq[(OrderDetail, Option[Product])]] = db.run {
    (for {
      (detail, product) <- orderdetail.filter(_.order === id) joinLeft prd on (_.product === _.id)
    } yield (detail, product)).result
  }

  def getById(id: Int): Future[Option[OrderDetail]] = db.run {
    orderdetail.filter(_.id === id).result.headOption
  }

  def deleteProductId(id: Int): Future[Unit] = {
    val orderDetailQuery = for{
      o <- orderdetail if o.product === id
    } yield o.product

     db.run(orderDetailQuery.update(None)).map(_ => ())
  }

  def update(id: Int, new_orderdetail: OrderDetail): Future[Unit] = {
    val orderDetailToUpdate: OrderDetail = new_orderdetail.copy(id)
    db.run(orderdetail.filter(_.id === id).update(orderDetailToUpdate)).map(_ => ())
  }

  def deleteByOrderId(id: Int): Future[Unit] = db.run(orderdetail.filter(_.order === id).delete).map(_ => ())

  def delete(id: Int): Future[Unit] = db.run(orderdetail.filter(_.id === id).delete).map(_ => ())
}