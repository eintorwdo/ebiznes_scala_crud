package controllers

import models.{User, UserRepository, Review, ReviewRepository, Order, OrderRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.DefaultEnv

import scala.concurrent._
import scala.util.{Failure, Success}
import play.api.libs.json._

@Singleton
class UserController @Inject()(userRepo: UserRepository, orderRepo: OrderRepository, reviewRepo: ReviewRepository, silhouette: Silhouette[DefaultEnv], cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  
  val updateUserForm: Form[UpdateUserForm] = Form {
    mapping(
      "id" -> nonEmptyText,
      "firstname" -> optional(text),
      "lastname" -> optional(text),
      "email" -> email,
      "password" -> optional(text),
      "role" -> nonEmptyText
    )(UpdateUserForm.apply)(UpdateUserForm.unapply)
  }

  val userForm: Form[CreateUserForm] = Form {
    mapping(
      "firstname" -> optional(text),
      "lastname" -> optional(text),
      "email" -> email,
      "password" -> optional(text),
      "role" -> nonEmptyText
    )(CreateUserForm.apply)(CreateUserForm.unapply)
  }

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

  def usersJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val usrQuery = userRepo.list()
    val users = Await.result(usrQuery, duration.Duration.Inf)
    val usersNoPwd = users.map(x => {Json.obj("id" -> x.id, "name" -> x.firstname, "email" -> x.email)})
    Ok(Json.obj("users" -> usersNoPwd))
  }

  def userJson(id: String) = Action { implicit request: MessagesRequest[AnyContent] =>
    val usrQuery = userRepo.getById(id)
    val user = Await.result(usrQuery, duration.Duration.Inf)
    if(user.nonEmpty){
      val ordQuery = orderRepo.getByUserId(id)
      val orders = Await.result(ordQuery, duration.Duration.Inf)
      val userNoPwd = Json.obj("id" -> user.get.id, "name" -> user.get.firstname, "email" -> user.get.email, "orders" -> orders)
      Ok(userNoPwd)
    }
    else{
      BadRequest(Json.obj("message" -> "User not found"))
    }
  }

  def users() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val usrs = userRepo.list()
    usrs.map(u => {
      Ok(views.html.users(u))
    })
  }

  def user(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val usr = userRepo.getById(id)
    usr.map(u => {
        if(u.nonEmpty){
          Ok(views.html.user(u.get))
        }
        else{
          BadRequest(views.html.index("User not found"))
        }
    })
  }

  def addUser() = Action { implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.useradd(userForm))
  }

  def addUserHandle = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.useradd(errorForm))
        )
      },
      user => {
        userRepo.create(user.firstname, user.lastname, user.email, user.password, user.role).map { _ =>
          Redirect(routes.UserController.addUser()).flashing("success" -> "User created")
        }
      }
    )
  }

  def updateUser(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val user = userRepo.getById(id)
    user.map(x => {
      if(x.nonEmpty){
        val usr = x.get
        val usrForm = updateUserForm.fill(UpdateUserForm(usr.id, usr.firstname, usr.lastname, usr.email, usr.password, usr.role))
        Ok(views.html.userupdate(usrForm))
      }
      else{
        BadRequest(views.html.index("User not found"))
      }
    })
  }

  def updateUserHandle = Action.async { implicit request =>
    updateUserForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.userupdate(errorForm))
        )
      },
      user => {
        userRepo.update(user.id, User(user.id, user.firstname, user.lastname, user.email, user.password, user.role)).map { _ =>
          Redirect(routes.UserController.updateUser(user.id)).flashing("success" -> "User updated")
        }
      }
    )
  }

  def deleteUser(id: String) = Action { implicit request =>
    val deleteOrders = orderRepo.deleteByUserId(id)
    Await.result(deleteOrders, duration.Duration.Inf)
    val deleteReviews = reviewRepo.deleteByUserId(id)
    Await.result(deleteReviews, duration.Duration.Inf)
    val deleteUser = userRepo.delete(id)
    Await.result(deleteUser, duration.Duration.Inf)
    Ok(views.html.index("User deleted"))
  }
}


case class CreateUserForm(firstname: Option[String], lastname: Option[String], email: String, password: Option[String], role: String)
case class UpdateUserForm(id: String, firstname: Option[String], lastname: Option[String], email: String, password: Option[String], role: String)
