package controllers

import models.{Category, CategoryRepository, SubCategory, SubCategoryRepository, Product, ProductRepository, Manufacturer, ManufacturerRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.{DefaultEnv, ResponseMsgs}

import scala.util.{Failure, Success}
import play.api.libs.json.JsPath.json

@Singleton
class CategoryController @Inject()(categoryRepo: CategoryRepository, subCategoryRepo: SubCategoryRepository, productsRepo: ProductRepository, manufacturerRepo: ManufacturerRepository, silhouette: Silhouette[DefaultEnv], cc: MessagesControllerComponents, messages: ResponseMsgs)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

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
      BadRequest(Json.obj("message" -> messages.catNotFound))
    }
  }

  def addCategoryJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
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
        BadRequest(Json.obj("message" -> messages.emptyBody))
      }
    }
    else{
      Forbidden(Json.obj("message" -> messages.notAuthorized))
    }
  }

  def updateCategoryJson(id: Int) = silhouette.SecuredAction.async { implicit request =>
    if(request.identity.role == "ADMIN"){
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
              Await.result(updateCat, duration.Duration.Inf)
              Ok(Json.toJson(newCat))
            }
          }
          else{
            BadRequest(Json.obj("message" -> messages.catNotFound))
          }
        })
      }
      else{
        Future.successful(
          BadRequest(Json.obj("message" -> messages.emptyBody))
        )
      }
    }
    else{
      Future(Forbidden(Json.obj("message" -> messages.notAuthorized)))
    }
  }

  def addSubCategoryJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson
      jsonBody match{
        case Some(obj) => {val name = (obj \ "name").validate[String].getOrElse("")
        val category = (obj \ "category").validate[Int].getOrElse(0)
        if(name.length == 0 || category == 0){
          BadRequest(Json.obj("message" -> "Incorrect name or category id"))
        }
        else{
          val catQuery = categoryRepo.getById(category)
          val catQueryRes = Await.result(catQuery, duration.Duration.Inf)
          catQueryRes match{
            case Some(_) => {
              val createSubCat = subCategoryRepo.create(name, category)
              val res = Await.result(createSubCat, duration.Duration.Inf)
              Ok(Json.toJson(res))
            }
            case None => BadRequest(Json.obj("message" -> "Invalid category id"))
          }
        }
      }
        case None => BadRequest(Json.obj("message" -> messages.emptyBody))
      }
    }
    else{
      Forbidden(Json.obj("message" -> messages.notAuthorized))
    }
  }

  def updateSubCategoryJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val subcategoryQuery = subCategoryRepo.getById(id)
      val jsonBody: Option[JsValue] = request.body.asJson
      val obj = jsonBody.getOrElse(Json.obj())
      val subcategory = Await.result(subcategoryQuery, duration.Duration.Inf)
      subcategory match{
        case Some(subcat) => {val newName = (obj \ "name").validate[String].getOrElse("")
        val newCategoryId = (obj \ "category").validate[Int].getOrElse(0)
        if(newName.length == 0 || newCategoryId == 0){
          BadRequest(Json.obj("message" -> "Invalid name or category id"))
        }
        else{
          val catQuery = categoryRepo.getById(newCategoryId)
          val catQueryRes = Await.result(catQuery, duration.Duration.Inf)
          catQueryRes match {
          case Some(_) => {val newSubCat = SubCategory(subcat.id, newName, newCategoryId)
            val updateSubCat = subCategoryRepo.update(id, newSubCat)
            Await.result(updateSubCat, duration.Duration.Inf)
            Ok(Json.toJson(newSubCat))
          }
          case None => BadRequest(Json.obj("message" -> "Invalid category id"))
          }
        }
      }
      case None => BadRequest(Json.obj("message" -> messages.catNotFound))
      }
    }
    else{
      Forbidden(Json.obj("message" -> messages.notAuthorized))
    }
  }

  def subCategoriesJson(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val subcategories = subCategoryRepo.list()
    subcategories.map(subcat => {
      Ok(Json.toJson(subcat))
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

  def deleteCategoryJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
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
    else{
      Forbidden(Json.obj("message" -> messages.notAuthorized))
    }
  }

  def deleteSubCategoryJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val deleteSubCatId = productsRepo.deleteSubCategoryId(id)
      Await.result(deleteSubCatId, duration.Duration.Inf)
      val deleteSubcategory = subCategoryRepo.delete(id)
      Await.result(deleteSubcategory, duration.Duration.Inf)
      Ok(Json.obj("message" -> "Subcategory deleted"))
    }
    else{
      Forbidden(Json.obj("message" -> messages.notAuthorized))
    }
  }
}
