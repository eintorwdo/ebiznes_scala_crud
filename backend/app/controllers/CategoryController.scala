package controllers

import models.{Category, CategoryRepository, SubCategory, SubCategoryRepository, Product, ProductRepository, Manufacturer, ManufacturerRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

// import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
// import play.api.libs.json.JsString

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class CategoryController @Inject()(categoryRepo: CategoryRepository, subCategoryRepo: SubCategoryRepository, productsRepo: ProductRepository, manufacturerRepo: ManufacturerRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

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

  def categoriesJson(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    val categories = categoryRepo.list()
    val res = Await.result(categories, duration.Duration.Inf)
    val catsWithSubcats = res.map(cat => {
      val subcats = subCategoryRepo.getByCategoryId(cat.id)
      val res2 = Await.result(subcats, duration.Duration.Inf)
      Json.obj("category" -> cat, "subcategories" -> res2)
    })
    Ok(Json.obj("categories" -> catsWithSubcats))
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

  def categoryJson(cat: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val catQuery = categoryRepo.getById(cat)
    val prodQuery = productsRepo.getByCategory(cat)
    val products = Await.result(prodQuery, duration.Duration.Inf)
    val category = Await.result(catQuery, duration.Duration.Inf)
    if(category.nonEmpty){
      val productsWithMan = products.map(p => {
        val manQuery = manufacturerRepo.getById(p.manufacturer.getOrElse(0))
        val manufacturer = Await.result(manQuery, duration.Duration.Inf)
        Json.obj("id" -> p.id, "name" -> p.name, "price" -> p.price, "amount" -> p.amount, "manufacturer" -> manufacturer.getOrElse(Manufacturer(0, "")).name)
      })
      Ok(Json.obj("category" -> category.get, "products" -> productsWithMan))
    }
    else{
      BadRequest(Json.obj("message" -> "Category not found"))
    }
  }

  def addCategoryJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson
    if(jsonBody.nonEmpty){
      val obj = jsonBody.get
      val nameLookup = (obj \ "name").validate[String]
      val name = nameLookup.getOrElse("")
      if(name.length == 0){
        BadRequest(Json.obj("message" -> "Invalid name"))
      }
      else{
        val createCat = categoryRepo.create(name)
        val res = Await.result(createCat, duration.Duration.Inf)
        Ok(Json.toJson(res))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty body"))
    }
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

  def updateCategoryJson(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val category = categoryRepo.getById(id)
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson
    if(jsonBody.nonEmpty){
      val obj = jsonBody.get
      category.map(x => {
        if(x.nonEmpty){
          val cat = x.get
          val nameLookup = (obj \ "name").validate[String]
          val newName = nameLookup.getOrElse("")
          if(newName.length == 0){
            BadRequest(Json.obj("message" -> "Invalid name"))
          }
          else{
            val newCat = Category(cat.id, newName)
            val updateCat = categoryRepo.update(id, newCat)
            val res = Await.result(updateCat, duration.Duration.Inf)
            Ok(Json.toJson(newCat))
          }
        }
        else{
          BadRequest(Json.obj("message" -> "Category not found"))
        }
      })
    }
    else{
      Future.successful(
        BadRequest(Json.obj("message" -> "Empty body"))
      )
    }
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

  def addSubCategoryJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson
    if(jsonBody.nonEmpty){
      val obj = jsonBody.get
      val nameLookup = (obj \ "name").validate[String]
      val categoryLookup = (obj \ "category").validate[Int]
      val name = nameLookup.getOrElse("")
      val category = categoryLookup.getOrElse(0)
      if(name.length == 0 || category == 0){
        BadRequest(Json.obj("message" -> "Incorrect name or category id"))
      }
      else{
        val catQuery = categoryRepo.getById(category)
        val catQueryRes = Await.result(catQuery, duration.Duration.Inf)
        if(catQueryRes.nonEmpty){
          val createSubCat = subCategoryRepo.create(name, category)
          val res = Await.result(createSubCat, duration.Duration.Inf)
          Ok(Json.toJson(res))
        }
        else{
          BadRequest(Json.obj("message" -> "Invalid category id"))
        }
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty body"))
    }
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

  def updateSubCategoryJson(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategory = subCategoryRepo.getById(id)
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson
    if(jsonBody.nonEmpty){
      val obj = jsonBody.get
      subcategory.map(x => {
        if(x.nonEmpty){
          val subcat = x.get
          val nameLookup = (obj \ "name").validate[String]
          val categoryLookup = (obj \ "category").validate[Int]
          val newName = nameLookup.getOrElse("")
          val newCategoryId = categoryLookup.getOrElse(0)
          if(newName.length == 0 || newCategoryId == 0){
            BadRequest(Json.obj("message" -> "Invalid name or category id"))
          }
          else{
            val catQuery = categoryRepo.getById(newCategoryId)
            val catQueryRes = Await.result(catQuery, duration.Duration.Inf)
            if(catQueryRes.nonEmpty){
              val newSubCat = SubCategory(subcat.id, newName, newCategoryId)
              val updateSubCat = subCategoryRepo.update(id, newSubCat)
              val res = Await.result(updateSubCat, duration.Duration.Inf)
              Ok(Json.toJson(newSubCat))
            }
            else{
              BadRequest(Json.obj("message" -> "Invalid category id"))
            }
          }
        }
        else{
          BadRequest(views.html.index("Category not found"))
        }
      })
    }
    else{
      Future.successful(
        BadRequest(Json.obj("message" -> "Empty body"))
      )
    }
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

  def subCategoriesJson(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategories = subCategoryRepo.list()
    subcategories.map(subcat => {
      Ok(Json.toJson(subcat))
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

  def subCategoryJson(subcat: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategory = subCategoryRepo.getById(subcat)
    val products = productsRepo.getBySubCategory(subcat)
    val result = for {
          r1 <- subcategory
          r2 <- products
    } yield (r1, r2)
    result.map(x => {
      if(x._1.nonEmpty){
        val category = categoryRepo.getById(x._1.get.category)
        val catResult = Await.result(category, duration.Duration.Inf)
        Ok(Json.obj("category" -> catResult.get,
                    "subcategory" -> x._1.get,
                    "products" -> x._2))
      }
      else{
        BadRequest(Json.obj("message" -> "Subcategory not found"))
      }
    })
  }

  def deleteCategoryJson(id: Int) = Action { implicit request =>
    val deleteSubCatId = productsRepo.deleteSubCategoryId(id)
    Await.result(deleteSubCatId, duration.Duration.Inf)
    val deleteCatId = productsRepo.deleteCategoryId(id)
    Await.result(deleteCatId, duration.Duration.Inf)
    val deleteSubcategories = subCategoryRepo.deleteByCategoryId(id)
    Await.result(deleteSubcategories, duration.Duration.Inf)
    val del = categoryRepo.delete(id)
    Await.result(del, duration.Duration.Inf)
    Ok(Json.obj("message" -> "Category deleted"))
  }

  def deleteSubCategoryJson(id: Int) = Action { implicit request =>
    val deleteSubCatId = productsRepo.deleteSubCategoryId(id)
    Await.result(deleteSubCatId, duration.Duration.Inf)
    val deleteSubcategory = subCategoryRepo.delete(id)
    Await.result(deleteSubcategory, duration.Duration.Inf)
    Ok(Json.obj("message" -> "Subcategory deleted"))
  }
}

case class CreateCategoryForm(name: String)
case class UpdateCategoryForm(id: Int, name: String)
case class CreateSubCategoryForm(name: String, category: Int)
case class UpdateSubCategoryForm(id: Int, name: String, category: Int)
