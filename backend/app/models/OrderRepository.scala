package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ Future, ExecutionContext }

@Singleton
class OrderRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val userRepository: UserRepository, val paymentRepository: PaymentRepository, val deliveryRepository: DeliveryRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._


  class OrderTable(tag: Tag) extends Table[Order](tag, "order_") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def price = column[Int]("price")

    /** The age column */
    def date = column[String]("date")

    def address = column[String]("address")

    def sent = column[Int]("sent")

    def user = column[Int]("user")

    def payment = column[Option[Int]]("payment")

    def delivery = column[Option[Int]]("delivery")

    def user_fk = foreignKey("usr_fk", user, usr)(_.id)

    def payment_fk = foreignKey("pmt_fk", payment, pmt)(_.id?)

    def delivery_fk = foreignKey("dlv_fk", delivery, dlv)(_.id?)

    def * = (id, price, date, address, sent, user, payment, delivery) <> ((Order.apply _).tupled, Order.unapply)

  }

  /**
   * The starting point for all queries on the people table.
   */

  import userRepository.UserTable
  import paymentRepository.PaymentTable
  import deliveryRepository.DeliveryTable

  val order = TableQuery[OrderTable]

  private val usr = TableQuery[UserTable]

  private val pmt = TableQuery[PaymentTable]

  private val dlv = TableQuery[DeliveryTable]


  /**
   * Create a person with the given name and age.
   *
   * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
   * id for that person.
   */
  def create(price: Int, date: String, address: String, sent: Int, user: Int, payment: Option[Int], delivery: Option[Int]): Future[Order] = db.run {
    (order.map(p => (p.price,p.date,p.address,p.sent,p.user,p.payment,p.delivery))
      returning order.map(_.id)
      into {case ((price,date,address,sent,user,payment,delivery),id) => Order(id,price,date,address,sent,user,payment,delivery)}
      ) += (price, date, address, sent, user, payment, delivery)
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Order]] = db.run {
    order.result
  }

  // def getById(id: Int): Future[Order] = db.run {
  //   order.filter(_.id === id).result.head
  // }

  def getByIdOption(id: Int): Future[Option[Order]] = db.run {
    order.filter(_.id === id).result.headOption
  }

  def delete(id: Int): Future[Unit] = db.run(order.filter(_.id === id).delete).map(_ => ())

  def update(id: Int, new_order: Order): Future[Unit] = {
    val orderToUpdate: Order = new_order.copy(id)
    db.run(order.filter(_.id === id).update(orderToUpdate)).map(_ => ())
  }

  def getById(id: Int): Future[Seq[(Order, User, Option[Payment], Option[Delivery])]] = db.run {
    (for {
      (((order, user), payment), delivery) <- order.filter(_.id === id) join usr on (_.user === _.id) joinLeft pmt on (_._1.payment === _.id) joinLeft dlv on (_._1._1.delivery === _.id)
    } yield (order, user, payment, delivery)).result
  }

  def deleteDeliveryId(id: Int): Future[Unit] = {
    val deliveryQuery = for{
      o <- order if o.delivery === id
    } yield o.delivery

     db.run(deliveryQuery.update(None)).map(_ => ())
  }

  def deletePaymentId(id: Int): Future[Unit] = {
    val paymentyQuery = for{
      o <- order if o.payment === id
    } yield o.payment

     db.run(paymentyQuery.update(None)).map(_ => ())
  }

  def deleteByUserId(id: Int): Future[Unit] = db.run(order.filter(_.user === id).delete).map(_ => ())
}