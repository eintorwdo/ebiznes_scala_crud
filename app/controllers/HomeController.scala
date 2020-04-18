package controllers

// import models.{Category, CategoryRepository, Product, ProductRepository}
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
class HomeController @Inject()()(val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def search(query: String) = Action { implicit request: Request[AnyContent] =>
    Ok(s"Wyszukales: $query")
  }

  def contact() = Action { implicit request: Request[AnyContent] =>
    Ok(s"Kontakt")
  }

}
