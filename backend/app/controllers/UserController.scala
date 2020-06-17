package controllers

import models.{User, UserRepository, Review, ReviewRepository, Order, OrderRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.{DefaultEnv, ResponseMsgs}

import scala.concurrent._
import scala.util.{Failure, Success}
import play.api.libs.json._

@Singleton
class UserController @Inject()(userRepo: UserRepository, orderRepo: OrderRepository, reviewRepo: ReviewRepository, silhouette: Silhouette[DefaultEnv], messages: ResponseMsgs, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  
  def getUserSecured() = silhouette.SecuredAction { implicit request =>
    val usrQuery = userRepo.getById(request.identity.id)
    val user = Await.result(usrQuery, duration.Duration.Inf)
    if(user.nonEmpty){
      val ordQuery = orderRepo.getByUserId(request.identity.id)
      val orders = Await.result(ordQuery, duration.Duration.Inf)
      val userNoPwd = Json.obj("id" -> user.get.id, "name" -> user.get.firstname, "email" -> user.get.email, "orders" -> orders)
      Ok(userNoPwd)
    }
    else{
      BadRequest(Json.obj("message" -> "User not found"))
    }
  }

  def usersJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val usrQuery = userRepo.list()
      val users = Await.result(usrQuery, duration.Duration.Inf)
      val usersNoPwd = users.map(x => {Json.obj("id" -> x.id, "name" -> x.firstname, "email" -> x.email)})
      Ok(Json.obj("users" -> usersNoPwd))
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def userJson(id: String) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val usrQuery = userRepo.getById(id)
      val user = Await.result(usrQuery, duration.Duration.Inf)
      if(user.nonEmpty){
        val ordQuery = orderRepo.getByUserId(id)
        val orders = Await.result(ordQuery, duration.Duration.Inf)
        val userNoPwd = Json.obj("id" -> user.get.id, "name" -> user.get.firstname, "lastname" -> user.get.lastname, "email" -> user.get.email, "role" -> user.get.role, "orders" -> orders)
        Ok(userNoPwd)
      }
      else{
        BadRequest(Json.obj("message" -> "User not found"))
      }
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def updateUserRole(id: String) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val user = userRepo.getById(id)
      val res = Await.result(user, duration.Duration.Inf)
      val json = request.body.asJson
      if(res.nonEmpty && json.nonEmpty){
        val oldUser = res.get
        val body = json.get
        val role = (body \ "role").validate[String].getOrElse("")
        if(role == "ADMIN" || role == "REGULAR"){
          val updateQuery = userRepo.update(id, User(id, oldUser.firstname, oldUser.lastname, oldUser.email, oldUser.password, role))
          Await.result(updateQuery, duration.Duration.Inf)
          Ok(Json.obj("message" -> "User role updated"))
        }
        else{
          BadRequest(Json.obj("message" -> "Invalid role parameter"))
        }
      }
      else{
        BadRequest(Json.obj("message" -> "User not found or empty request body"))
      }
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def deleteUser(id: String) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val deleteOrders = orderRepo.deleteByUserId(id)
      Await.result(deleteOrders, duration.Duration.Inf)
      val deleteReviews = reviewRepo.deleteByUserId(id)
      Await.result(deleteReviews, duration.Duration.Inf)
      val deleteUser = userRepo.delete(id)
      Await.result(deleteUser, duration.Duration.Inf)
      Ok(Json.obj("message" -> "User deleted"))
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }
}
