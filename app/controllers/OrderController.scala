package controllers

import models.{Order, OrderRepository, OrderDetail, OrderDetailRepository, Delivery, DeliveryRepository, Payment, PaymentRepository}
import javax.inject._
import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class OrderController @Inject()(orderRepo: OrderRepository, orderDetailRepo: OrderDetailRepository, deliveryRepo: DeliveryRepository, paymentRepo: PaymentRepository)(val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {
  def orders() = Action.async { implicit request: Request[AnyContent] =>
  val ordrs = orderRepo.list()
    ordrs.map(cat => {
      Ok(s"Zamowienia: $cat")
    })
  }

  def order(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val ord = orderRepo.getById(id.toInt)
    val orderdetail = orderDetailRepo.getByOrderId(id.toInt)
    val result = for{
      r1 <- ord
      r2 <- orderdetail
    } yield (r1, r2)

    result.map(c => {
         Ok(s"Zamowienie: ${c._1}\nSzczegoly: ${c._2}")
    })
  }

  def deliveries() = Action.async { implicit request: Request[AnyContent] =>
  val dlvs = deliveryRepo.list()
    dlvs.map(cat => {
      Ok(s"Rodzaje dostaw: $cat")
    })
  }

  def payments() = Action.async { implicit request: Request[AnyContent] =>
  val pmts = paymentRepo.list()
    pmts.map(cat => {
      Ok(s"Rodzaje platnosci: $cat")
    })
  }

  def delivery(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val dlv = deliveryRepo.getById(id.toInt)
    dlv.map(c => {
         Ok(s"Dostawa: $c")
    })
  }

  def payment(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val pmt = paymentRepo.getById(id.toInt)
    pmt.map(c => {
         Ok(s"Platnosc: $c")
    })
  }
}
