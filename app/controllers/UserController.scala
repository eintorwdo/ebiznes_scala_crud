package controllers

import models.{User, UserRepository, Review, ReviewRepository, Order, OrderRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent._
import scala.util.{Failure, Success}


@Singleton
class UserController @Inject()(userRepo: UserRepository, orderRepo: OrderRepository, reviewRepo: ReviewRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  
  val updateUserForm: Form[UpdateUserForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(UpdateUserForm.apply)(UpdateUserForm.unapply)
  }

  val userForm: Form[CreateUserForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(CreateUserForm.apply)(CreateUserForm.unapply)
  }



  def users() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val usrs = userRepo.list()
    usrs.map(u => {
      Ok(views.html.users(u))
    })
  }

  def user(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val usr = userRepo.getById(id.toInt)
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
        userRepo.create(user.name, user.email, user.password).map { _ =>
          Redirect(routes.UserController.addUser()).flashing("success" -> "User created")
        }
      }
    )
  }

  def updateUser(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val user = userRepo.getById(id)
    user.map(x => {
      if(x.nonEmpty){
        val usr = x.get
        val usrForm = updateUserForm.fill(UpdateUserForm(usr.id, usr.name, usr.email, usr.password))
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
        userRepo.update(user.id, User(user.id, user.name, user.email, user.password)).map { _ =>
          Redirect(routes.UserController.updateUser(user.id)).flashing("success" -> "User updated")
        }
      }
    )
  }

  def deleteUser(id: Int) = Action { implicit request =>
    val deleteOrders = orderRepo.deleteByUserId(id)
    Await.result(deleteOrders, duration.Duration.Inf)
    val deleteReviews = reviewRepo.deleteByUserId(id)
    Await.result(deleteReviews, duration.Duration.Inf)
    val deleteUser = userRepo.delete(id)
    Await.result(deleteUser, duration.Duration.Inf)
    Ok(views.html.index("User deleted"))
  }
}


case class CreateUserForm(name: String, email: String, password: String)
case class UpdateUserForm(id: Int, name: String, email: String, password: String)
