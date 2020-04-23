package controllers

import models.{Category, CategoryRepository, SubCategory, SubCategoryRepository, Product, ProductRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.data.Form
import play.api.data.Forms._

// import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class CategoryController @Inject()(categoryRepo: CategoryRepository, subCategoryRepo: SubCategoryRepository, productsRepo: ProductRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

  val updateCategoryForm: Form[UpdateCategoryForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText
    )(UpdateCategoryForm.apply)(UpdateCategoryForm.unapply)
  }

  val categoryForm: Form[CreateCategoryForm] = Form {
    mapping(
      "name" -> nonEmptyText
    )(CreateCategoryForm.apply)(CreateCategoryForm.unapply)
  }

  val updateSubCategoryForm: Form[UpdateSubCategoryForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText,
      "category" -> number
    )(UpdateSubCategoryForm.apply)(UpdateSubCategoryForm.unapply)
  }

  val subCategoryForm: Form[CreateSubCategoryForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "category" -> number
    )(CreateSubCategoryForm.apply)(CreateSubCategoryForm.unapply)
  }


  def categories(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val categories = categoryRepo.list()
    categories.map(cat => {
      Ok(views.html.categories(cat))
    })
  }

  def category(cat: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val category = categoryRepo.getById(cat)
    val products = productsRepo.getByCategory(cat)
    val result = for {
          r1 <- category
          r2 <- products
    } yield (r1, r2)
    result.map(x => {
      if(x._1.nonEmpty){
        Ok(views.html.category(x._1.get, x._2))
      }
      else{
        BadRequest(views.html.index("Category not found"))
      }
    })
  }

  def addCategory() = Action { implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.categoryadd(categoryForm))
  }

  def addCategoryHandle = Action.async { implicit request =>
    categoryForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.categoryadd(errorForm))
        )
      },
      category => {
        categoryRepo.create(category.name).map { _ =>
          Redirect(routes.CategoryController.addCategory()).flashing("success" -> "Category created")
        }
      }
    )
  }

  def updateCategory(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val category = categoryRepo.getById(id)
    category.map(x => {
      if(x.nonEmpty){
        val cat = x.get
        val catForm = updateCategoryForm.fill(UpdateCategoryForm(cat.id, cat.name))
        Ok(views.html.categoryupdate(catForm))
      }
      else{
        BadRequest(views.html.index("Category not found"))
      }
    })
  }

  def updateCategoryHandle = Action.async { implicit request =>
    updateCategoryForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.categoryupdate(errorForm))
        )
      },
      category => {
        categoryRepo.update(category.id, Category(category.id, category.name)).map { _ =>
          Redirect(routes.CategoryController.updateCategory(category.id)).flashing("success" -> "Category updated")
        }
      }
    )
  }

  def addSubCategory() = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val categories = categoryRepo.list() 
    categories.map(x => {
      if(x.nonEmpty){
        Ok(views.html.subcategoryadd(subCategoryForm, x))
      }
      else{
        BadRequest(views.html.index("No categories found. Cannot add subcategory"))
      }
    })
  }

  def addSubCategoryHandle = Action.async { implicit request =>
    val categories = categoryRepo.list() 
    val res = Await.result(categories, duration.Duration.Inf)
    subCategoryForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.subcategoryadd(errorForm, res))
        )
      },
      subcategory => {
        subCategoryRepo.create(subcategory.name, subcategory.category).map { _ =>
          Redirect(routes.CategoryController.addSubCategory()).flashing("success" -> "Subcategory created")
        }
      }
    )
  }

  def updateSubCategory(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategory = subCategoryRepo.getById(id)
    val categories = categoryRepo.list()
    val res = Await.result(categories, duration.Duration.Inf)
    subcategory.map(x => {
      if(x.nonEmpty){
        val subcat = x.get
        val subCatForm = updateSubCategoryForm.fill(UpdateSubCategoryForm(subcat.id, subcat.name, subcat.category))
        Ok(views.html.subcategoryupdate(subCatForm, res))
      }
      else{
        BadRequest(views.html.index("Subcategory not found"))
      }
    })
  }

  def updateSubCategoryHandle = Action.async { implicit request =>
    val categories = categoryRepo.list()
    val res = Await.result(categories, duration.Duration.Inf)
    updateSubCategoryForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.subcategoryupdate(errorForm, res))
        )
      },
      subcategory => {
        subCategoryRepo.update(subcategory.id, SubCategory(subcategory.id, subcategory.name, subcategory.category)).map { _ =>
          Redirect(routes.CategoryController.updateSubCategory(subcategory.id)).flashing("success" -> "Subcategory updated")
        }
      }
    )
  }

  def subcategories(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategories = subCategoryRepo.list()
    subcategories.map(subcat => {
      Ok(views.html.subcategories(subcat))
    })
  }

  def subcategory(cat: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategory = subCategoryRepo.getById(cat)
    val products = productsRepo.getBySubCategory(cat)
    val result = for {
          r1 <- subcategory
          r2 <- products
    } yield (r1, r2)
    result.map(x => {
      if(x._1.nonEmpty){
        Ok(views.html.subcategory(x._1.get, x._2))
      }
      else{
        BadRequest(views.html.index("Subcategory not found"))
      }
    })
  }

  def deleteCategory(id: Int) = Action { implicit request =>
    val deleteSubCatId = productsRepo.deleteSubCategoryId(id)
    Await.result(deleteSubCatId, duration.Duration.Inf)
    val deleteCatId = productsRepo.deleteCategoryId(id)
    Await.result(deleteCatId, duration.Duration.Inf)
    val deleteSubcategories = subCategoryRepo.deleteByCategoryId(id)
    Await.result(deleteSubcategories, duration.Duration.Inf)
    val del = categoryRepo.delete(id)
    Await.result(del, duration.Duration.Inf)
    Ok(views.html.index("Category deleted"))
  }

  def deleteSubCategory(id: Int) = Action { implicit request =>
    val deleteSubCatId = productsRepo.deleteSubCategoryId(id)
    Await.result(deleteSubCatId, duration.Duration.Inf)
    val deleteSubcategories = subCategoryRepo.deleteByCategoryId(id)
    Await.result(deleteSubcategories, duration.Duration.Inf)
    Ok(views.html.index("Subcategory deleted"))
  }
}

case class CreateCategoryForm(name: String)
case class UpdateCategoryForm(id: Int, name: String)
case class CreateSubCategoryForm(name: String, category: Int)
case class UpdateSubCategoryForm(id: Int, name: String, category: Int)
