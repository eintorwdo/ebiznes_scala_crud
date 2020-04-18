package controllers

import models.{User, UserRepository}
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
class UserController @Inject()(userRepo: UserRepository)(val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {
  def users() = Action.async { implicit request: Request[AnyContent] =>
  val usrs = userRepo.list()
    usrs.map(cat => {
      Ok(s"Uzytkownicy: $cat")
    })
  }

  def user(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val usr = userRepo.getById(id.toInt)
    usr.map(cat => {
         Ok(s"Uzytkownik: $cat")
    })
  }
}
