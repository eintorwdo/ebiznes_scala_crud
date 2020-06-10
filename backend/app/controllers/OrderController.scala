package controllers

import models.{Order, OrderRepository, OrderDetail, OrderDetailRepository, Delivery, DeliveryRepository, Payment, PaymentRepository, User, UserRepository, Product, ProductRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

import scala.concurrent._
import scala.util.{Failure, Success}
import slick.dbio.DBIOAction
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.DefaultEnv

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class OrderController @Inject()(productRepo: ProductRepository, userRepo: UserRepository, orderRepo: OrderRepository, orderDetailRepo: OrderDetailRepository, deliveryRepo: DeliveryRepository, paymentRepo: PaymentRepository, silhouette: Silhouette[DefaultEnv], cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  
  val updateOrderDetailForm: Form[UpdateOrderDetailForm] = Form {
    mapping(
      "id" -> number,
      "price" -> number,
      "order" -> number,
      "product" -> optional(number),
      "amount" -> number
    )(UpdateOrderDetailForm.apply)(UpdateOrderDetailForm.unapply)
  }

  val orderDetailForm: Form[CreateOrderDetailForm] = Form {
    mapping(
      "price" -> number,
      "order" -> number,
      "product" -> optional(number),
      "amount" -> number
    )(CreateOrderDetailForm.apply)(CreateOrderDetailForm.unapply)
  }

  val updateOrderForm: Form[UpdateOrderForm] = Form {
    mapping(
      "id" -> number,
      "price" -> number,
      "address" -> nonEmptyText,
      "sent" -> number,
      "payment" -> optional(number),
      "delivery" -> optional(number),
      "paid" -> number,
      "packageNr" -> nonEmptyText
    )(UpdateOrderForm.apply)(UpdateOrderForm.unapply)
  }

  val orderForm: Form[CreateOrderForm] = Form {
    mapping(
      "price" -> number,
      "address" -> nonEmptyText,
      "sent" -> number,
      "user" -> nonEmptyText,
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

  def ordersJson() = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val ordrs = orderRepo.list()
    ordrs.map(o => {
      Ok(Json.toJson(o))
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

  def orderJson(id: Int) = silhouette.SecuredAction.async { implicit request =>
    val ord = orderRepo.getById(id)
    val orderdetail = orderDetailRepo.getByOrderId(id)
    val result = for{
      r1 <- ord
      r2 <- orderdetail
    } yield (r1, r2)

    result.map(c => {
      if(c._1.nonEmpty){
        if(c._1.head._2.id == request.identity.id || request.identity.role == "ADMIN"){
          val details = c._2.map(d => {
            val prd = d._2.getOrElse(Product(0, "", "", 0, 0, Option(0), Option(0), Option(0)))
            Json.obj("name" -> prd.name, "price" -> d._1.price, "amount" -> d._1.amount)
          })
          var resJson = Json.obj("info" -> c._1.head._1)
          resJson = resJson + ("user" -> Json.toJson(c._1.head._2))
          if(c._1.head._3.isDefined) resJson = resJson + ("payment" -> Json.toJson(c._1.head._3.get)) else resJson = resJson + ("payment" -> JsNull)
          if(c._1.head._4.isDefined) resJson = resJson + ("delivery" -> Json.toJson(c._1.head._4.get)) else resJson = resJson + ("delivery" -> JsNull)
          Ok(Json.obj("order" -> resJson,
                      "details" -> details))
        }
        else{
          Status(UNAUTHORIZED)
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Order not found"))
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

  def addOrderJson() = silhouette.SecuredAction { implicit request =>
    val date = java.time.LocalDate.now.toString
    val json = request.body.asJson
    if(json.nonEmpty){
      val body = json.get
      val detailsJs = (body \ "details").validate[Seq[JsObject]]
      val addressJs = (body \ "address").validate[String]
      val paymentJs = (body \ "payment").validate[Int]
      val deliveryJs = (body \ "delivery").validate[Int]

      val details = detailsJs.getOrElse(Seq())
      val address = addressJs.getOrElse("")
      val payment = paymentJs.getOrElse(0)
      val delivery = deliveryJs.getOrElse(0)

      if(details.nonEmpty && address != "" && payment != 0 && delivery != 0){
        val ids = details.map(d => {
          val idJs = (d \ "id").validate[Int]
          val amountJs = (d \ "amount").validate[Int]
          val id = idJs.getOrElse(0)
          val amount = amountJs.getOrElse(0)
          (id, amount)
        })
        var areDetailsValid = true
        ids.foreach(el => {
          if(el._1 <= 0 || el._2 <= 0){
            areDetailsValid = false
          }
        })
        if(!areDetailsValid){
          BadRequest(Json.obj("message" -> "Invalid product id or amount specified"))
        }
        else{
          val updateProducts = productRepo.updateProductsAfterOrder(ids)
          val updatedRows = Await.result(updateProducts, duration.Duration.Inf)
          updatedRows.length match {
            case 0 => {
              BadRequest(Json.obj("message" -> "One of the products is not available"))
            }
            case _ => {
              var totalPrice = 0
              val productQuery = productRepo.productsWithIds(ids.map(p => p._1).toList)
              val products = Await.result(productQuery, duration.Duration.Inf)
              val orderDetails = ids.map(i => {
                val product = products.filter(p => p.id == i._1).head
                totalPrice = totalPrice + product.price * i._2
                (product.price, 0, product.id, i._2)
              })
              val deliveryQuery = deliveryRepo.getById(delivery)
              val deliveryResult = Await.result(deliveryQuery, duration.Duration.Inf)
              val paymentQuery = paymentRepo.getById(payment)
              val paymentResult = Await.result(paymentQuery, duration.Duration.Inf)
              if(deliveryResult.nonEmpty && paymentResult.nonEmpty){
                totalPrice = totalPrice + deliveryResult.get.price
                val paid = 0
                val packageNr = ""
                val sent = 0
                val createOrderQuery = orderRepo.create(totalPrice, date, address, sent, request.identity.id, Some(payment), Some(delivery), paid, packageNr)
                val newOrder = Await.result(createOrderQuery, duration.Duration.Inf)
                val detailsWithOrderId = orderDetails.map(d => (d._1, newOrder.id, Some(d._3), d._4))
                val detailsInsertQuery = orderDetailRepo.insertMany(detailsWithOrderId)
                val insertedDetails = Await.result(detailsInsertQuery, duration.Duration.Inf)
                Ok(Json.obj("inserted" -> insertedDetails))
              }
              else{
                BadRequest(Json.obj("message" -> "Invalid payment or delivery id"))
              }
            }
          }
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body"))
    }
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
        val paid = 0
        val packageNr = ""
        orderRepo.create(order.price, date, order.address, order.sent, order.user, order.payment, order.delivery, paid, packageNr).map { _ =>
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
      val ordForm = updateOrderForm.fill(UpdateOrderForm(ord._1.id, ord._1.price, ord._1.address, ord._1.sent, ord._1.payment, ord._1.delivery, ord._1.paid, ord._1.packageNr))
      Ok(views.html.orderupdate(ordForm, res3, res2))
    }
    else{
      BadRequest(views.html.index("Order not found"))
    }
  }

  def updateOrderJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val order = orderRepo.getById(id)
    val res = Await.result(order, duration.Duration.Inf)
    val json = request.body.asJson
    if(res.nonEmpty && json.nonEmpty){
      val body = json.get
      val delIdJs = (body \ "deliveryId").validate[Int]
      val pmtIdJs = (body \ "paymentId").validate[Int]
      val addrJs = (body \ "address").validate[String]
      val sntJs = (body \ "sent").validate[Int]
      val prcJs = (body \ "price").validate[Int]
      val paidJs = (body \ "paid").validate[Int]
      val packageJs = (body \ "packageNr").validate[String]
      val delId = delIdJs.getOrElse(0)
      val pmtId = pmtIdJs.getOrElse(0)
      val addr = addrJs.getOrElse("")
      val snt = sntJs.getOrElse(-1)
      val prc = prcJs.getOrElse(-1)
      val paid = paidJs.getOrElse(-1)
      val packageNr = packageJs.getOrElse("")
      val ord = res.head
      if(delId == 0 || pmtId == 0 || addr == "" || snt == -1 || prc < 0 || paid < 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val delivery = deliveryRepo.getById(delId)
        val res2 = Await.result(delivery, duration.Duration.Inf)
        val payment = paymentRepo.getById(pmtId)
        val res3 = Await.result(payment, duration.Duration.Inf)
        if(res2.isEmpty || res3.isEmpty){
          BadRequest(Json.obj("message" -> "Invalid delivery or payment id"))
        }
        else{
          val newOrd = Order(ord._1.id, prc, ord._1.date, addr, snt, ord._1.user, Option(pmtId), Option(delId), paid, packageNr)
          val orderUpdate = orderRepo.update(id, newOrd)
          Await.result(orderUpdate, duration.Duration.Inf)
          Ok(Json.toJson(newOrd))
        }
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Order not found or request body empty"))
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
            orderRepo.update(order.id, Order(order.id, order.price, res.head._1.date, order.address, order.sent, res.head._1.user, order.payment, order.delivery, order.paid, order.packageNr)).map { _ =>
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
      BadRequest(views.html.index("Order not found"))
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
          orderDetailRepo.create(orderdetail.price, orderdetail.order, orderdetail.product, orderdetail.amount).map { _ =>
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
      val ordForm = updateOrderDetailForm.fill(UpdateOrderDetailForm(res3.head.id, res3.head.price, res3.head.order, res3.head.product, res3.head.amount))
      Ok(views.html.orderdetailupdate(ordForm, res, res2))
    }
    else{
      BadRequest(views.html.index("Order detail not found"))
    }
  }

  // def updateOrderDetailJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
  //   val orderdetail = orderDetailRepo.getById(id)
  //   val res3 = Await.result(orderdetail, duration.Duration.Inf)
  //   val json = request.body.asJson
  //   if(res3.nonEmpty && json.nonEmpty){
  //     val body = json.get
  //     val priceJs = (body \ "price").validate[Int]
  //     val price = priceJs.getOrElse(-1)
  //     if(price < 0){
  //       BadRequest(Json.obj("message" -> "Invalid price value"))
  //     }
  //     else{
  //       val newDetail = OrderDetail(res3.get.id, price, res3.get.order, res3.get.product)
  //       val detailUpdate = orderDetailRepo.update(id, newDetail)
  //       Await.result(detailUpdate, duration.Duration.Inf)
  //       val orderQuery = orderRepo.getById(newDetail.order)
  //       val orderRes = Await.result(orderQuery, duration.Duration.Inf)
  //       val order = orderRes.head._1
  //       val newOrderPrice = order.price + (price-res3.get.price)
  //       val newOrder = Order(order.id, newOrderPrice, order.date, order.address, order.sent, order.user, order.payment, order.delivery, order.paid, order.packageNr)
  //       val updateOrder = orderRepo.update(order.id, newOrder)
  //       Await.result(updateOrder, duration.Duration.Inf)
  //       Ok(Json.toJson(newDetail))
  //     }
  //   }
  //   else{
  //     BadRequest(Json.obj("message" -> "Order detail not found or empty request body"))
  //   }
  // }

  def orderDetailsJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = orderDetailRepo.list()
    val orderdetails = Await.result(query, duration.Duration.Inf)
    Ok(Json.toJson(orderdetails))
  }

  def orderDetailJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = orderDetailRepo.getById(id)
    val orderdetail = Await.result(query, duration.Duration.Inf)
    if(orderdetail.nonEmpty){
      Ok(Json.toJson(orderdetail.get))
    }
    else{
      BadRequest(Json.obj("message" -> "Order detail not found"))
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
          orderDetailRepo.update(orderdetail.id, OrderDetail(orderdetail.id, orderdetail.price, orderdetail.order, orderdetail.product, orderdetail.amount)).map { _ =>
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

  def deliveriesJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = deliveryRepo.list()
    val deliveries = Await.result(query, duration.Duration.Inf)
    Ok(Json.toJson(deliveries))
  }

  def deliveryJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = deliveryRepo.getById(id)
    val delivery = Await.result(query, duration.Duration.Inf)
    if(delivery.nonEmpty){
      Ok(Json.toJson(delivery.get))
    }
    else{
      BadRequest(Json.obj("message" -> "Delivery not found"))
    }
  }

  def addDeliveryJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val json = request.body.asJson
    if(json.nonEmpty){
      val body = json.get
      val nameJs = (body \ "name").validate[String]
      val priceJs = (body \ "price").validate[Int]
      val name = nameJs.getOrElse("")
      val price = priceJs.getOrElse(-1)
      if(name == "" || price < 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val createDelivery = deliveryRepo.create(name, price)
        val delivery = Await.result(createDelivery, duration.Duration.Inf)
        Ok(Json.toJson(delivery))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body"))
    }
  }

  def updateDeliveryJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val delivery = deliveryRepo.getById(id)
    val res = Await.result(delivery, duration.Duration.Inf)
    val json = request.body.asJson
    if(res.nonEmpty && json.nonEmpty){
      val x = res.get
      val body = json.get
      val nameJs = (body \ "name").validate[String]
      val priceJs = (body \ "price").validate[Int]
      val name = nameJs.getOrElse("")
      val price = priceJs.getOrElse(-1)
      if(name == "" || price < 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val newDelivery = Delivery(id, name, price)
        val deliveryUpdate = deliveryRepo.update(id, newDelivery)
        Await.result(deliveryUpdate, duration.Duration.Inf)
        Ok(Json.toJson(newDelivery))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body or delivery not found"))
    }
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

  def paymentsJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = paymentRepo.list()
    val payments = Await.result(query, duration.Duration.Inf)
    Ok(Json.toJson(payments))
  }

  def paymentJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = paymentRepo.getById(id)
    val payment = Await.result(query, duration.Duration.Inf)
    if(payment.nonEmpty){
      Ok(Json.toJson(payment.get))
    }
    else{
      BadRequest(Json.obj("message" -> "Payment not found"))
    }
  }

  def addPaymentJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val json = request.body.asJson
    if(json.nonEmpty){
      val body = json.get
      val nameJs = (body \ "name").validate[String]
      val name = nameJs.getOrElse("")
      if(name == ""){
        BadRequest(Json.obj("message" -> "Invalid name"))
      }
      else{
        val createPayment = paymentRepo.create(name)
        val payment = Await.result(createPayment, duration.Duration.Inf)
        Ok(Json.toJson(payment))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body"))
    }
  }

  def updatePaymentJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val payment = paymentRepo.getById(id)
    val res = Await.result(payment, duration.Duration.Inf)
    val json = request.body.asJson
    if(res.nonEmpty && json.nonEmpty){
      val x = res.get
      val body = json.get
      val nameJs = (body \ "name").validate[String]
      val name = nameJs.getOrElse("")
      if(name == ""){
        BadRequest(Json.obj("message" -> "Invalid name"))
      }
      else{
        val newPayment = Payment(id, name)
        val paymentUpdate = paymentRepo.update(id, newPayment)
        Await.result(paymentUpdate, duration.Duration.Inf)
        Ok(Json.toJson(newPayment))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body or payment not found"))
    }
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

case class CreateOrderForm(price: Int, address: String, sent: Int, user: String, payment: Option[Int], delivery: Option[Int])
case class UpdateOrderForm(id: Int, price: Int, address: String, sent: Int, payment: Option[Int], delivery: Option[Int], paid: Int, packageNr: String)
case class CreateOrderDetailForm(price: Int, order: Int, product: Option[Int], amount: Int)
case class UpdateOrderDetailForm(id: Int, price: Int, order: Int, product: Option[Int], amount: Int)
case class CreateDeliveryForm(name: String, price: Int)
case class UpdateDeliveryForm(id: Int, name: String, price: Int)
case class CreatePaymentForm(name: String)
case class UpdatePaymentForm(id: Int, name: String)