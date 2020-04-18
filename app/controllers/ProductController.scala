package controllers

import models.{Category, CategoryRepository, Product, ProductRepository, ManufacturerRepository, Manufacturer, SubCategoryRepository, SubCategory, Review, ReviewRepository}
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
class ProductController @Inject()(productsRepo: ProductRepository, categoryRepo: CategoryRepository, manufacturerRepo: ManufacturerRepository, subCategoryRepository: SubCategoryRepository, reviewRepo: ReviewRepository)(val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {

  def products() = Action.async { implicit request: Request[AnyContent] =>
  val produkty = productsRepo.list()
    produkty.map(cat => {
      Ok(s"Produkty: $cat")
    })
  }

  def product(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val produkt = productsRepo.getById(id.toInt)
    produkt.map(cat => {
         Ok(s"Produkt: $cat")
    })
  }

  def manufacturers() = Action.async { implicit request: Request[AnyContent] =>
  val manufacturers = manufacturerRepo.list()
    manufacturers.map(cat => {
      Ok(s"Producenci: $cat")
    })
  }

  def manufacturer(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val manu = manufacturerRepo.getById(id.toInt)
    manu.map(cat => {
         Ok(s"Producent: $cat")
    })
  }

  def reviews() = Action.async { implicit request: Request[AnyContent] =>
  val revs = reviewRepo.list()
    revs.map(cat => {
      Ok(s"Opinie: $cat")
    })
  }

  def review(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val rev = reviewRepo.getById(id.toInt)
    rev.map(cat => {
         Ok(s"Opinia: $cat")
    })
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

  def checkout() = Action { implicit request: Request[AnyContent] =>
    Ok(s"Zakupiono cos. Teraz zaplac")
  }

}
