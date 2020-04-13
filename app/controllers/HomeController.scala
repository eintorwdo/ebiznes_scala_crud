package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def search(query: String) = Action { implicit request: Request[AnyContent] =>
    Ok(s"Wyszukales: $query")
  }

  def product(id: String) = Action { implicit request: Request[AnyContent] =>
    Ok(s"ID produktu: $id")
  }

  def categories() = Action { implicit request: Request[AnyContent] =>
    Ok(s"Kategorie: ")
  }

  def category(cat: String) = Action { implicit request: Request[AnyContent] =>
    Ok(s"Kategoria: $cat")
  }

  def addToCart(id: String) = Action { implicit request: Request[AnyContent] =>
    Ok(s"Dodales do koszyka: $id")
  }

  def removeFromCart(id: String) = Action { implicit request: Request[AnyContent] =>
    Ok(s"Usunales z koszyka: $id")
  }

  def cart() = Action { implicit request: Request[AnyContent] =>
    Ok(s"Koszyk")
  }

  def contact() = Action { implicit request: Request[AnyContent] =>
    Ok(s"Kontakt")
  }

  def checkout() = Action { implicit request: Request[AnyContent] =>
    Ok(s"Zakupiono cos. Teraz zaplac")
  }
}
