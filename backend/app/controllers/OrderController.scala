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
import utils.{DefaultEnv, ResponseMsgs, Validation}

@Singleton
class OrderController @Inject()(productRepo: ProductRepository, userRepo: UserRepository, orderRepo: OrderRepository, orderDetailRepo: OrderDetailRepository, deliveryRepo: DeliveryRepository, paymentRepo: PaymentRepository, silhouette: Silhouette[DefaultEnv], messages: ResponseMsgs, validation: Validation, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  
  def ordersJson() = silhouette.SecuredAction.async { implicit request =>
    if(request.identity.role == "ADMIN"){
      val ordrs = orderRepo.list()
      ordrs.map(o => {
        Ok(Json.toJson(o))
      })
    }
    else{
      Future(Forbidden(messages.notAuthorized))
    }
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
          val user = Json.obj("id" -> c._1.head._2.id, "email" -> c._1.head._2.email)
          resJson = resJson + ("user" -> user)
          if(c._1.head._3.isDefined) resJson = resJson + ("payment" -> Json.toJson(c._1.head._3.get)) else resJson = resJson + ("payment" -> JsNull)
          if(c._1.head._4.isDefined) resJson = resJson + ("delivery" -> Json.toJson(c._1.head._4.get)) else resJson = resJson + ("delivery" -> JsNull)
          Ok(Json.obj("order" -> resJson,
                      "details" -> details))
        }
        else{
          Forbidden(messages.notAuthorized)
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Order not found"))
      }
    })
  }

  def addOrderJson() = silhouette.SecuredAction { implicit request =>
    val date = java.time.LocalDate.now.toString
    val json = request.body.asJson
    val body = json.getOrElse(Json.obj())
    val details = (body \ "details").validate[Seq[JsObject]].getOrElse(Seq())
    val address = (body \ "address").validate[String].getOrElse("")
    val payment = (body \ "payment").validate[Int].getOrElse(0)
    val delivery = (body \ "delivery").validate[Int].getOrElse(0)
    val deliveryQuery = deliveryRepo.getById(delivery)
    val deliveryResult = Await.result(deliveryQuery, duration.Duration.Inf)
    val paymentQuery = paymentRepo.getById(payment)
    val paymentResult = Await.result(paymentQuery, duration.Duration.Inf)
    if(details.nonEmpty && address != "" && paymentResult.nonEmpty && deliveryResult.nonEmpty){
      val ids = details.map(d => {
        val id = (d \ "id").validate[Int].getOrElse(0)
        val amount = (d \ "amount").validate[Int].getOrElse(0)
        (id, amount)
      })
      val areDetailsValid = validation.areDetailsValid(ids)
      areDetailsValid match{
        case false => BadRequest(Json.obj("message" -> "Invalid product id or amount specified"))
        case true => {
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
          }
        }
      }
    }
    else{
      BadRequest(messages.invalidBody)
    }
  }

  def updateOrderJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val order = orderRepo.getById(id)
      val res = Await.result(order, duration.Duration.Inf)
      val json = request.body.asJson
      if(res.nonEmpty && json.nonEmpty){
        val body = json.get
        val addr = (body \ "address").validate[String].getOrElse("")
        val snt = (body \ "sent").validate[Int].getOrElse(-1)
        val prc = (body \ "price").validate[Int].getOrElse(-1)
        val paid = (body \ "paid").validate[Int].getOrElse(-1)
        val packageNr = (body \ "packageNr").validate[String].getOrElse("")
        val ord = res.head
        if(addr == "" || snt < 0 || prc < 0 || paid < 0){
          BadRequest(messages.invalidBody)
        }
        else{
          val newOrd = Order(ord._1.id, prc, ord._1.date, addr, snt, ord._1.user, ord._1.payment, ord._1.delivery, paid, packageNr)
          val orderUpdate = orderRepo.update(id, newOrd)
          Await.result(orderUpdate, duration.Duration.Inf)
          Ok(Json.toJson(newOrd))
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Order not found or request body empty"))
      }
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def orderDetailJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val query = orderDetailRepo.getById(id)
      val orderdetail = Await.result(query, duration.Duration.Inf)
      if(orderdetail.nonEmpty){
        Ok(Json.toJson(orderdetail.get))
      }
      else{
        BadRequest(Json.obj("message" -> "Order detail not found"))
      }
    }
    else{
      Forbidden(messages.notAuthorized)
    }
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

  def addDeliveryJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val json = request.body.asJson
      if(json.nonEmpty){
        val body = json.get
        val name = (body \ "name").validate[String].getOrElse("")
        val price = (body \ "price").validate[Int].getOrElse(-1)
        if(name == "" || price < 0){
          BadRequest(messages.invalidBody)
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
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def updateDeliveryJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val delivery = deliveryRepo.getById(id)
      val res = Await.result(delivery, duration.Duration.Inf)
      val json = request.body.asJson
      if(res.nonEmpty && json.nonEmpty){
        val body = json.get
        val name = (body \ "name").validate[String].getOrElse("")
        val price = (body \ "price").validate[Int].getOrElse(-1)
        if(name == "" || price < 0){
          BadRequest(messages.invalidBody)
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
    else{
      Forbidden(messages.notAuthorized)
    }
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

  def addPaymentJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val json = request.body.asJson
      if(json.nonEmpty){
        val body = json.get
        val name = (body \ "name").validate[String].getOrElse("")
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
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def updatePaymentJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val payment = paymentRepo.getById(id)
      val res = Await.result(payment, duration.Duration.Inf)
      val json = request.body.asJson
      if(res.nonEmpty && json.nonEmpty){
        val body = json.get
        val name = (body \ "name").validate[String].getOrElse("")
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
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def deleteOrderJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val deleteOrderDetail = orderDetailRepo.deleteByOrderId(id)
      Await.result(deleteOrderDetail, duration.Duration.Inf)
      val deleteOrder = orderRepo.delete(id)
      Await.result(deleteOrder, duration.Duration.Inf)
      Ok(Json.obj("message" -> "Order deleted"))
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }
}
