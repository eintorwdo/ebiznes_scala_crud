package controllers

import models.{Category, CategoryRepository, SubCategory, SubCategoryRepository}
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
class CategoryController @Inject()(categoryRepo: CategoryRepository, subCategoryRepo: SubCategoryRepository)(val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends BaseController {

  def categories(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val kategorie = categoryRepo.list()
    kategorie.map(cat => {
      Ok(s"Kategorie: $cat")
    })
  }

  def category(cat: String) = Action.async { implicit request: Request[AnyContent] =>
    val kategoria = categoryRepo.getById(cat.toInt)
    kategoria.map(cat => {
      val test = cat.name
      Ok(s"Kategoria: $test")
    })
  }

  def subcategories(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val podkategorie = subCategoryRepo.list()
    podkategorie.map(cat => {
      Ok(s"Podkategorie: $cat")
    })
  }

  def subcategory(cat: String) = Action.async { implicit request: Request[AnyContent] =>
    val podkategoria = subCategoryRepo.getById(cat.toInt)
    podkategoria.map(cat => {
      val test = cat.name
      Ok(s"Podkategoria: $test")
    })
  }
}
