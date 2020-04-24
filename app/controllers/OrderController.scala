package controllers

import models.{Order, OrderRepository, OrderDetail, OrderDetailRepository, Delivery, DeliveryRepository, Payment, PaymentRepository, User, UserRepository, Product, ProductRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent._
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class OrderController @Inject()(productRepo: ProductRepository, userRepo: UserRepository, orderRepo: OrderRepository, orderDetailRepo: OrderDetailRepository, deliveryRepo: DeliveryRepository, paymentRepo: PaymentRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  
  val updateOrderDetailForm: Form[UpdateOrderDetailForm] = Form {
    mapping(
      "id" -> number,
      "price" -> number,
      "order" -> number,
      "product" -> optional(number)
    )(UpdateOrderDetailForm.apply)(UpdateOrderDetailForm.unapply)
  }

  val orderDetailForm: Form[CreateOrderDetailForm] = Form {
    mapping(
      "price" -> number,
      "order" -> number,
      "product" -> optional(number)
    )(CreateOrderDetailForm.apply)(CreateOrderDetailForm.unapply)
  }

  val updateOrderForm: Form[UpdateOrderForm] = Form {
    mapping(
      "id" -> number,
      "price" -> number,
      "address" -> nonEmptyText,
      "sent" -> number,
      "payment" -> optional(number),
      "delivery" -> optional(number)
    )(UpdateOrderForm.apply)(UpdateOrderForm.unapply)
  }

  val orderForm: Form[CreateOrderForm] = Form {
    mapping(
      "price" -> number,
      "address" -> nonEmptyText,
      "sent" -> number,
      "user" -> number,
      "payment" -> optional(number),
      "delivery" -> optional(number)
    )(CreateOrderForm.apply)(CreateOrderForm.unapply)
  }

  val updateDeliveryForm: Form[UpdateDeliveryForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText,
      "price" -> number
    )(UpdateDeliveryForm.apply)(UpdateDeliveryForm.unapply)
  }

  val deliveryForm: Form[CreateDeliveryForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "price" -> number
    )(CreateDeliveryForm.apply)(CreateDeliveryForm.unapply)
  }

  val updatePaymentForm: Form[UpdatePaymentForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText
    )(UpdatePaymentForm.apply)(UpdatePaymentForm.unapply)
  }

  val paymentForm: Form[CreatePaymentForm] = Form {
    mapping(
      "name" -> nonEmptyText
    )(CreatePaymentForm.apply)(CreatePaymentForm.unapply)
  }

  
  def orders() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val ordrs = orderRepo.list()
    ordrs.map(o => {
      Ok(views.html.orders(o))
    })
  }

  def order(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val ord = orderRepo.getById(id)
    val orderdetail = orderDetailRepo.getByOrderId(id)
    val result = for{
      r1 <- ord
      r2 <- orderdetail
    } yield (r1, r2)

    result.map(c => {
      if(c._1.nonEmpty){
        val res = (c._1.head, c._2)
        Ok(views.html.order(res))
      }
      else{
        BadRequest(views.html.index("Order not found"))
      }
    })
  }

  def addOrder() = Action { implicit request: MessagesRequest[AnyContent] =>
    val users = userRepo.list()
    val res = Await.result(users, duration.Duration.Inf)
    val deliveries = deliveryRepo.list()
    val res2 = Await.result(deliveries, duration.Duration.Inf)
    val payments = paymentRepo.list()
    val res3 = Await.result(payments, duration.Duration.Inf)
    val date = java.time.LocalDate.now.toString
    Ok(views.html.orderadd(orderForm, res, res3, res2))
  }

  def addOrderHandle = Action.async { implicit request =>
    val users = userRepo.list()
    val res = Await.result(users, duration.Duration.Inf)
    val deliveries = deliveryRepo.list()
    val res2 = Await.result(deliveries, duration.Duration.Inf)
    val payments = paymentRepo.list()
    val res3 = Await.result(payments, duration.Duration.Inf)
    orderForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.orderadd(errorForm, res, res3, res2))
        )
      },
      order => {
        val date = java.time.LocalDate.now.toString
        orderRepo.create(order.price, date, order.address, order.sent, order.user, order.payment, order.delivery).map { _ =>
          Redirect(routes.OrderController.addOrder()).flashing("success" -> "Order created")
        }
      }
    )
  }

  def updateOrder(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val order = orderRepo.getById(id)
    val res = Await.result(order, duration.Duration.Inf)
    val deliveries = deliveryRepo.list()
    val res2 = Await.result(deliveries, duration.Duration.Inf)
    val payments = paymentRepo.list()
    val res3 = Await.result(payments, duration.Duration.Inf)
    val date = java.time.LocalDate.now.toString
    if(res.nonEmpty){
      val ord = res.head
      val ordForm = updateOrderForm.fill(UpdateOrderForm(ord._1.id, ord._1.price, ord._1.address, ord._1.sent, ord._1.payment, ord._1.delivery))
      Ok(views.html.orderupdate(ordForm, res3, res2))
    }
    else{
      BadRequest(views.html.index("Order not found"))
    }
  }

  def updateOrderHandle() = Action.async { implicit request =>
    val deliveries = deliveryRepo.list()
    val res2 = Await.result(deliveries, duration.Duration.Inf)
    val payments = paymentRepo.list()
    val res3 = Await.result(payments, duration.Duration.Inf)
      updateOrderForm.bindFromRequest.fold(
        errorForm => {
          Future.successful(
            BadRequest(views.html.orderupdate(errorForm, res3, res2))
          )
        },
        order => {
          val ord = orderRepo.getById(order.id)
          val res = Await.result(ord, duration.Duration.Inf)
          if(res.nonEmpty){
            orderRepo.update(order.id, Order(order.id, order.price, res.head._1.date, order.address, order.sent, res.head._1.user, order.payment, order.delivery)).map { _ =>
              Redirect(routes.OrderController.updateOrder(order.id)).flashing("success" -> "Order updated")
            }
          }
          else{
            Future.successful(
              BadRequest(views.html.index("Order not found"))
            )
          }
        }
      )
  }

  def addOrderDetail(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val order = orderRepo.getByIdOption(id)
    val res = Await.result(order, duration.Duration.Inf)
    val products = productRepo.list()
    val res2 = Await.result(products, duration.Duration.Inf)
    if(res.nonEmpty){
      Ok(views.html.orderdetailadd(orderDetailForm, id, res2))
    }
    else{
      BadRequest(views.html.index("No orders found"))
    }
  }

  def addOrderDetailHandle(id: Int) = Action.async { implicit request =>
    val products = productRepo.list()
    val res2 = Await.result(products, duration.Duration.Inf)
    orderDetailForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.orderdetailadd(errorForm, id, res2))
        )
      },
      orderdetail => {
        val order = orderRepo.getByIdOption(id)
        val res = Await.result(order, duration.Duration.Inf)
        if(res.nonEmpty){
          orderDetailRepo.create(orderdetail.price, orderdetail.order, orderdetail.product).map { _ =>
            Redirect(routes.OrderController.order(id))
          }
        }
        else{
          Future.successful(
            BadRequest(views.html.index("Order not found"))
          )
        }
      }
    )
  }

  def updateOrderDetail(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val orders = orderRepo.list()
    val res = Await.result(orders, duration.Duration.Inf)
    val products = productRepo.list()
    val res2 = Await.result(products, duration.Duration.Inf)
    val orderdetail = orderDetailRepo.getById(id)
    val res3 = Await.result(orderdetail, duration.Duration.Inf)
    if(res3.nonEmpty){
      val ordForm = updateOrderDetailForm.fill(UpdateOrderDetailForm(res3.head.id, res3.head.price, res3.head.order, res3.head.product))
      Ok(views.html.orderdetailupdate(ordForm, res, res2))
    }
    else{
      BadRequest(views.html.index("Order detail not found"))
    }
  }

  def updateOrderDetailHandle() = Action.async { implicit request =>
    val orders = orderRepo.list()
    val res = Await.result(orders, duration.Duration.Inf)
    val products = productRepo.list()
    val res2 = Await.result(products, duration.Duration.Inf)
    updateOrderDetailForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.orderdetailupdate(errorForm, res, res2))
        )
      },
      orderdetail => {
        val ord = orderDetailRepo.getById(orderdetail.id)
        val res3 = Await.result(ord, duration.Duration.Inf)
        if(res3.nonEmpty){
          orderDetailRepo.update(orderdetail.id, OrderDetail(orderdetail.id, orderdetail.price, orderdetail.order, orderdetail.product)).map { _ =>
            Redirect(routes.OrderController.order(orderdetail.order))
          }
        }
        else{
          Future.successful(
            BadRequest(views.html.index("Order detail not found"))
          )
        }
      }
    )
  }

  def addDelivery() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.deliveryadd(deliveryForm))
  }

  def addDeliveryHandle = Action.async { implicit request =>
    deliveryForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.deliveryadd(errorForm))
        )
      },
      delivery => {
        deliveryRepo.create(delivery.name, delivery.price).map { _ =>
          Redirect(routes.OrderController.addDelivery()).flashing("success" -> "Delivery created")
        }
      }
    )
  }

  def updateDelivery(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val delivery = deliveryRepo.getById(id)
    val res = Await.result(delivery, duration.Duration.Inf)
    if(res.nonEmpty){
      val x = res.get
      val delForm = updateDeliveryForm.fill(UpdateDeliveryForm(x.id, x.name, x.price))
      Ok(views.html.deliveryupdate(delForm))
    }
    else{
      BadRequest(views.html.index("Delivery not found"))
    }
  }

  def updateDeliveryHandle() = Action.async { implicit request =>
    updateDeliveryForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.deliveryupdate(errorForm))
        )
      },
      delivery => {
        val del = deliveryRepo.getById(delivery.id)
        val res = Await.result(del, duration.Duration.Inf)
        if(res.nonEmpty){
          deliveryRepo.update(delivery.id, Delivery(delivery.id, delivery.name, delivery.price)).map { _ =>
            Redirect(routes.OrderController.updateDelivery(delivery.id)).flashing("success" -> "Delivery updated")
          }
        }
        else{
          Future.successful(
            BadRequest(views.html.index("Delivery not found"))
          )
        }
      }
    )
  }

  def addPayment() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.paymentadd(paymentForm))
  }

  def addPaymentHandle = Action.async { implicit request =>
    paymentForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.paymentadd(errorForm))
        )
      },
      payment => {
        paymentRepo.create(payment.name).map { _ =>
          Redirect(routes.OrderController.addPayment()).flashing("success" -> "Payment method created")
        }
      }
    )
  }

  def updatePayment(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val payment = paymentRepo.getById(id)
    val res = Await.result(payment, duration.Duration.Inf)
    if(res.nonEmpty){
      val x = res.get
      val pmtForm = updatePaymentForm.fill(UpdatePaymentForm(x.id, x.name))
      Ok(views.html.paymentupdate(pmtForm))
    }
    else{
      BadRequest(views.html.index("Payment method not found"))
    }
  }

  def updatePaymentHandle() = Action.async { implicit request =>
    updatePaymentForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.paymentupdate(errorForm))
        )
      },
      payment => {
        val pmt = paymentRepo.getById(payment.id)
        val res = Await.result(pmt, duration.Duration.Inf)
        if(res.nonEmpty){
          paymentRepo.update(payment.id, Payment(payment.id, payment.name)).map { _ =>
            Redirect(routes.OrderController.updatePayment(payment.id)).flashing("success" -> "Payment method updated")
          }
        }
        else{
          Future.successful(
            BadRequest(views.html.index("Payment method not found"))
          )
        }
      }
    )
  }


  def deliveries() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val dlvs = deliveryRepo.list()
    dlvs.map(cat => {
      Ok(s"Rodzaje dostaw: $cat")
    })
  }

  def payments() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val pmts = paymentRepo.list()
    pmts.map(cat => {
      Ok(s"Rodzaje platnosci: $cat")
    })
  }

  def delivery(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val dlv = deliveryRepo.getById(id.toInt)
    dlv.map(c => {
         Ok(s"Dostawa: $c")
    })
  }

  def payment(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val pmt = paymentRepo.getById(id.toInt)
    pmt.map(c => {
         Ok(s"Platnosc: $c")
    })
  }

  def deleteOrder(id: Int) = Action { implicit request =>
    val deleteOrderDetail = orderDetailRepo.deleteByOrderId(id)
    Await.result(deleteOrderDetail, duration.Duration.Inf)
    val deleteOrder = orderRepo.delete(id)
    Await.result(deleteOrder, duration.Duration.Inf)
    Ok(views.html.index("Order deleted"))
  }

  def deleteOrderDetail(id: Int) = Action { implicit request =>
    val deleteOrderDetail = orderDetailRepo.delete(id)
    Await.result(deleteOrderDetail, duration.Duration.Inf)
    Ok(views.html.index("Order detail deleted"))
  }

  def deleteDelivery(id: Int) = Action { implicit request =>
    val deleteInOrder = orderRepo.deleteDeliveryId(id)
    Await.result(deleteInOrder, duration.Duration.Inf)
    val deleteDelivery = deliveryRepo.delete(id)
    Await.result(deleteDelivery, duration.Duration.Inf)
    Ok(views.html.index("Delivery deleted"))
  }

  def deletePayment(id: Int) = Action { implicit request =>
    val deleteInOrder = orderRepo.deletePaymentId(id)
    Await.result(deleteInOrder, duration.Duration.Inf)
    val deletePayment = paymentRepo.delete(id)
    Await.result(deletePayment, duration.Duration.Inf)
    Ok(views.html.index("Payment deleted"))
  }
}

case class CreateOrderForm(price: Int, address: String, sent: Int, user: Int, payment: Option[Int], delivery: Option[Int])
case class UpdateOrderForm(id: Int, price: Int, address: String, sent: Int, payment: Option[Int], delivery: Option[Int])
case class CreateOrderDetailForm(price: Int, order: Int, product: Option[Int])
case class UpdateOrderDetailForm(id: Int, price: Int, order: Int, product: Option[Int])
case class CreateDeliveryForm(name: String, price: Int)
case class UpdateDeliveryForm(id: Int, name: String, price: Int)
case class CreatePaymentForm(name: String)
case class UpdatePaymentForm(id: Int, name: String)