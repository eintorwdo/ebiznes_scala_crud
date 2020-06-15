package controllers

import models.{Category, CategoryRepository, Product, ProductRepository, ManufacturerRepository, Manufacturer, SubCategoryRepository, SubCategory, Review, ReviewRepository, User, UserRepository, Order, OrderDetailRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent._
import scala.util.{Failure, Success}
import akka.protobufv3.internal.Duration
import play.api.libs.json._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.DefaultEnv

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProductController @Inject()(productsRepo: ProductRepository, categoryRepo: CategoryRepository, manufacturerRepo: ManufacturerRepository, subCategoryRepository: SubCategoryRepository, reviewRepo: ReviewRepository, userRepo: UserRepository, orderDetailRepo: OrderDetailRepository, silhouette: Silhouette[DefaultEnv], cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

  def mapProduct(p: (Int, String, String, Int, Int, Option[Int], Option[Int], Option[Int]), category: String) = {
    Json.obj("id" -> p._1, "name" -> p._2, "description" -> p._3, "price" -> p._4, "amount" -> p._5, "category" -> category)
  }

  def randProductsJson(cat1: Option[Int], cat2: Option[Int], cat3: Option[Int]) = Action { implicit request: MessagesRequest[AnyContent] =>
    val id1 = cat1.getOrElse(0)
    val id2 = cat2.getOrElse(0)
    val id3 = cat3.getOrElse(0)
    if(id1 == 0 || id2 == 0 || id3 == 0){
      BadRequest(Json.obj("message" -> "Invalid request body"))
    }
    else{
    val catQuery1 = categoryRepo.getById(id1)
    val catQuery2 = categoryRepo.getById(id2)
    val catQuery3 = categoryRepo.getById(id3)
    val category1 = Await.result(catQuery1, duration.Duration.Inf)
    val category2 = Await.result(catQuery2, duration.Duration.Inf)
    val category3 = Await.result(catQuery3, duration.Duration.Inf)
    val cat1Name = category1.getOrElse(Category(0, "")).name
    val cat2Name = category2.getOrElse(Category(0, "")).name
    val cat3Name = category3.getOrElse(Category(0, "")).name
    val dbQuery1 = productsRepo.getRandomProducts(id1)
    val dbQuery2 = productsRepo.getRandomProducts(id2)
    val dbQuery3 = productsRepo.getRandomProducts(id3)
    val products1 = Await.result(dbQuery1, duration.Duration.Inf)
    val products2 = Await.result(dbQuery2, duration.Duration.Inf)
    val products3 = Await.result(dbQuery3, duration.Duration.Inf)
    val products1Js = products1.map(p => mapProduct(p, cat1Name))
    val products2Js = products2.map(p => mapProduct(p, cat2Name))
    val products3Js = products3.map(p => mapProduct(p, cat3Name))
    Ok(Json.obj("products1" -> products1Js, "products2" -> products2Js, "products3" -> products3Js, "categories" -> JsArray(Seq(Json.obj("name" -> cat1Name), Json.obj("name" -> cat2Name), Json.obj("name" -> cat3Name)))))
    }
  }

  def productsJson(query: Option[String], category: Option[Int], subcategory: Option[Int]) = Action { implicit request: MessagesRequest[AnyContent] =>
    val searchQuery = query.getOrElse("")
    val cat = category.getOrElse(0)
    val subcat = subcategory.getOrElse(0)
    var dbQuery = productsRepo.list()
    if(searchQuery != ""){
      dbQuery = productsRepo.search(searchQuery)
    }
    if(cat != 0){
      dbQuery = productsRepo.searchInCategory(searchQuery, cat)
    }
    if(subcat != 0){
      dbQuery = productsRepo.searchInSubCategory(searchQuery, subcat)
    }
    val products = Await.result(dbQuery, duration.Duration.Inf)
    val productsWithMan = products.map(p => {
      val manQuery = manufacturerRepo.getById(p.manufacturer.getOrElse(0))
      val manufacturer = Await.result(manQuery, duration.Duration.Inf)
      Json.obj("id" -> p.id, "name" -> p.name, "price" -> p.price, "amount" -> p.amount, "manufacturer" -> manufacturer.getOrElse(Manufacturer(0, "")).name)
    })
    Ok(Json.obj("products" -> productsWithMan))
  }

  def productJson(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val product = productsRepo.getById(id)
    val reviews = reviewRepo.listByProdId(id)
    val result = for {
          r1 <- product
          r2 <- reviews
    } yield (r1, r2)
    result.map(res => {
      if(res._1.isEmpty){
        BadRequest(Json.obj("message" -> "Product not found"))
      }
      else{
        val revs = res._2.map(x => {Json.obj("id" -> x._1.id, "description" -> x._1.description, "userid" -> x._1.user, "username" -> x._2.email, "date" -> x._1.date)})
        Ok(Json.obj("product" -> Json.obj("info" -> res._1.head._1, "manufacturer" -> res._1.head._2, "category" -> res._1.head._3, "subcategory" -> res._1.head._4),
                    "reviews" -> revs))
      }
    })
  }

  def productsWithIdsJson(id: List[Int]) = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = productsRepo.productsWithIds(id)
    val products = Await.result(query, duration.Duration.Inf)
    Ok(Json.obj("products" -> products))
  }

  def addProductJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
    val json = request.body.asJson
      if(json.nonEmpty){
        val body = json.get
        val catIdJs = (body \ "category").validate[Int]
        val subIdJs = (body \ "subcategory").validate[Int]
        val manIdJs = (body \ "manufacturer").validate[Int]
        val nameJs = (body \ "name").validate[String]
        val dscJs = (body \ "description").validate[String]
        val prcJs = (body \ "price").validate[Int]
        val amountJs = (body \ "amount").validate[Int]
        val catId = catIdJs.getOrElse(0)
        val subId = subIdJs.getOrElse(0)
        val manId = manIdJs.getOrElse(0)
        val name = nameJs.getOrElse("")
        val description = dscJs.getOrElse("")
        val price = prcJs.getOrElse(0)
        val amount = amountJs.getOrElse(-1)
        if(catId == 0 || subId == 0 || manId == 0 || name == "" || description == "" || price <= 0 || amount < 0){
          BadRequest(Json.obj("message" -> "Invalid request body"))
        }
        else{
          val catQuery = categoryRepo.getById(catId)
          val subQuery = subCategoryRepository.getById(subId)
          val manQuery = manufacturerRepo.getById(manId)
          val category = Await.result(catQuery, duration.Duration.Inf)
          val subcat = Await.result(subQuery, duration.Duration.Inf)
          val man = Await.result(manQuery, duration.Duration.Inf)
          if(category.isEmpty || subcat.isEmpty || man.isEmpty){
            BadRequest(Json.obj("message" -> "Invalid request body"))
          }
          else{
            val newPrd = productsRepo.create(name, description, price, amount, Option(manId), Option(catId), Option(subId))
            val product = Await.result(newPrd, duration.Duration.Inf)
            Ok(Json.toJson(product))
          }
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Empty request body"))
      }
    }
    else{
      Forbidden(Json.obj("message" -> "Not authorized"))
    }
  }

  def updateProductJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val json = request.body.asJson
      val query = productsRepo.getById(id)
      val oldProduct = Await.result(query, duration.Duration.Inf)
      if(json.nonEmpty && oldProduct.nonEmpty){
        val body = json.get
        val catIdJs = (body \ "category").validate[Int]
        val subIdJs = (body \ "subcategory").validate[Int]
        val manIdJs = (body \ "manufacturer").validate[Int]
        val nameJs = (body \ "name").validate[String]
        val dscJs = (body \ "description").validate[String]
        val prcJs = (body \ "price").validate[Int]
        val amountJs = (body \ "amount").validate[Int]
        val catId = catIdJs.getOrElse(0)
        val subId = subIdJs.getOrElse(0)
        val manId = manIdJs.getOrElse(0)
        val name = nameJs.getOrElse("")
        val description = dscJs.getOrElse("")
        val price = prcJs.getOrElse(0)
        val amount = amountJs.getOrElse(-1)
        if(catId == 0 || manId == 0 || name == "" || description == "" || price <= 0 || amount < 0){
          BadRequest(Json.obj("message" -> "Invalid request body"))
        }
        else{
          val catQuery = categoryRepo.getById(catId)
          val subQuery = subCategoryRepository.getById(subId)
          val manQuery = manufacturerRepo.getById(manId)
          val category = Await.result(catQuery, duration.Duration.Inf)
          val subcat = Await.result(subQuery, duration.Duration.Inf)
          val man = Await.result(manQuery, duration.Duration.Inf)
          if(category.isEmpty || man.isEmpty){
            BadRequest(Json.obj("message" -> "Invalid request body"))
          }
          else{
            val newProduct = Product(oldProduct.head._1.id, name, description, price, amount, Option(manId), Option(catId), Option(subId))
            val updateQuery = productsRepo.update(oldProduct.head._1.id, newProduct)
            Await.result(updateQuery, duration.Duration.Inf)
            Ok(Json.toJson(newProduct))
          }
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Empty request body or product not found"))
      }
    }
    else{
      Forbidden(Json.obj("message" -> "Not authorized"))
    }
  }

  def manufacturersJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = manufacturerRepo.list()
    val manufacturers = Await.result(query, duration.Duration.Inf)
    Ok(Json.toJson(manufacturers))
  }

  def manufacturerJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = manufacturerRepo.getById(id)
    val manufacturer = Await.result(query, duration.Duration.Inf)
    if(manufacturer.nonEmpty){
      Ok(Json.toJson(manufacturer.get))
    }
    else{
      BadRequest(Json.obj("message" -> "Manufacturer not found"))
    }
  }

  def addManufacturerJson() = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){  
      val json = request.body.asJson
      if(json.nonEmpty){
        val body = json.get
        val nameJs = (body \ "name").validate[String]
        val name = nameJs.getOrElse("")
        if(name == ""){
          BadRequest(Json.obj("message" -> "Invalid name parameter"))
        }
        else{
          val query = manufacturerRepo.create(name)
          val result = Await.result(query, duration.Duration.Inf)
          Ok(Json.toJson(result))
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Empty request body"))
      }
    }
    else{
      Forbidden(Json.obj("message" -> "Not authorized"))
    }
  }

  def updateManufacturerJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val manQuery = manufacturerRepo.getById(id)
      val oldMan = Await.result(manQuery, duration.Duration.Inf)
      val json = request.body.asJson
      if(json.nonEmpty && oldMan.nonEmpty){
        val body = json.get
        val nameJs = (body \ "name").validate[String]
        val name = nameJs.getOrElse("")
        if(name == ""){
          BadRequest(Json.obj("message" -> "Invalid name parameter"))
        }
        else{
          val newMan = Manufacturer(id, name)
          val query = manufacturerRepo.update(id, newMan)
          Await.result(query, duration.Duration.Inf)
          Ok(Json.toJson(newMan))
        }
      }
      else{
        BadRequest(Json.obj("message" -> "Empty request body"))
      }
    }
    else{
      Forbidden(Json.obj("message" -> "Not authorized"))
    }
  }

  def reviewsJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = reviewRepo.list()
    val reviews = Await.result(query, duration.Duration.Inf)
    Ok(Json.toJson(reviews))
  }

  def reviewJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val query = reviewRepo.getById(id)
    val review = Await.result(query, duration.Duration.Inf)
    if(review.nonEmpty){
      Ok(Json.toJson(review.get))
    }
    else{
      BadRequest(Json.obj("message" -> "Review not found"))
    }
  }

  def addReviewJson() = silhouette.SecuredAction { implicit request =>
    val json = request.body.asJson
    if(json.nonEmpty){
      val body = json.get
      val dscJs = (body \ "description").validate[String]
      val prdJs = (body \ "product").validate[Int]
      val description = dscJs.getOrElse("")
      val product = prdJs.getOrElse(0)
      if(description == "" || product == 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val prdQuery = productsRepo.getById(product)
        val prdRes = Await.result(prdQuery, duration.Duration.Inf)
        if(prdRes.nonEmpty){
          val date = java.time.LocalDate.now.toString
          val createRev = reviewRepo.create(description, request.identity.id, product, date)
          val rev = Await.result(createRev, duration.Duration.Inf)
          Ok(Json.obj("id" -> rev.id, "date" -> rev.date, "description" -> rev.description, "username" -> request.identity.email))
        }
        else{
          BadRequest(Json.obj("message" -> "Invalid request body"))
        }
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body"))
    }
  }

  def updateReviewJson(id: Int) = silhouette.SecuredAction { implicit request =>
    val oldRevQuery = reviewRepo.getById(id)
    val oldRev = Await.result(oldRevQuery, duration.Duration.Inf)
    val json = request.body.asJson
    if(json.nonEmpty && oldRev.nonEmpty){
      if(oldRev.get.user == request.identity.id){
        val body = json.get
        val dscJs = (body \ "description").validate[String]
        val prdJs = (body \ "product").validate[Int]
        val description = dscJs.getOrElse("")
        val product = prdJs.getOrElse(0)
        if(description == "" || product == 0){
          BadRequest(Json.obj("message" -> "Invalid request body"))
        }
        else{
          val prdQuery = productsRepo.getById(product)
          val prdRes = Await.result(prdQuery, duration.Duration.Inf)
          if(prdRes.nonEmpty){
            val newRev = Review(id, description, request.identity.id, product, oldRev.get.date)
            val updateRev = reviewRepo.update(id, newRev)
            Await.result(updateRev, duration.Duration.Inf)
            Ok(Json.toJson(newRev))
          }
          else{
            BadRequest(Json.obj("message" -> "Invalid request body"))
          }
        }
      }
      else{
        Forbidden(Json.obj("message" -> "Not authorized"))
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body or review not found"))
    }
  }

  // def deleteReview(id: Int) = Action { implicit request =>
  //   val del = reviewRepo.delete(id)
  //   Await.result(del, duration.Duration.Inf)
  //   Ok(views.html.index("Review deleted"))
  // }

  def deleteProductJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val delRev = reviewRepo.deleteByProductId(id)
      Await.result(delRev, duration.Duration.Inf)
      val updateOrd = orderDetailRepo.deleteProductId(id)
      Await.result(updateOrd, duration.Duration.Inf)
      val delPrd = productsRepo.delete(id)
      Await.result(delPrd, duration.Duration.Inf)
      Ok(Json.obj("message" -> "Product deleted"))
    }
    else{
      Forbidden(Json.obj("message" -> "Not authorized"))
    }
  }
}
