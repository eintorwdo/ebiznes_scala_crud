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
import utils.{DefaultEnv, ResponseMsgs}

@Singleton
class ProductController @Inject()(productsRepo: ProductRepository, categoryRepo: CategoryRepository, manufacturerRepo: ManufacturerRepository, subCategoryRepository: SubCategoryRepository, reviewRepo: ReviewRepository, userRepo: UserRepository, orderDetailRepo: OrderDetailRepository, silhouette: Silhouette[DefaultEnv], messages: ResponseMsgs, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

  def mapProduct(p: (Int, String, String, Int, Int, Option[Int], Option[Int], Option[Int]), category: String) = {
    Json.obj("id" -> p._1, "name" -> p._2, "description" -> p._3, "price" -> p._4, "amount" -> p._5, "category" -> category)
  }

  def randProductsJson(cat1: Option[Int], cat2: Option[Int], cat3: Option[Int]) = Action { implicit request: MessagesRequest[AnyContent] =>
    val id1 = cat1.getOrElse(0)
    val id2 = cat2.getOrElse(0)
    val id3 = cat3.getOrElse(0)
    if(id1 == 0 || id2 == 0 || id3 == 0){
      BadRequest(messages.invalidBody)
    }
    else{
    val catQuery1 = categoryRepo.getById(id1)
    val catQuery2 = categoryRepo.getById(id2)
    val catQuery3 = categoryRepo.getById(id3)
    val aggCategories = for{
      f1Result <- catQuery1
      f2Result <- catQuery2
      f3Result <- catQuery3
    } yield Seq(f1Result, f2Result, f3Result)
    val catResults = Await.result(aggCategories, duration.Duration.Inf)
    val categoryNames = catResults.map(c => {
      if(c.nonEmpty){
        c.get.name
      }
      else{
        ""
      }
    })
    val dbQuery1 = productsRepo.getRandomProducts(id1)
    val dbQuery2 = productsRepo.getRandomProducts(id2)
    val dbQuery3 = productsRepo.getRandomProducts(id3)
    val aggProducts = for{
      f1Result <- dbQuery1
      f2Result <- dbQuery2
      f3Result <- dbQuery3
    } yield Seq(f1Result, f2Result, f3Result)
    val prdResults = Await.result(aggProducts, duration.Duration.Inf)
    val prdCategoryTuples = prdResults.map(prds => prds.zipWithIndex.map{case (p,i) => mapProduct(p, categoryNames(i))})
    Ok(Json.obj("products1" -> prdCategoryTuples(0), "products2" -> prdCategoryTuples(1), "products3" -> prdCategoryTuples(2), "categories" -> JsArray(Seq(Json.obj("name" -> categoryNames(0)), Json.obj("name" -> categoryNames(1)), Json.obj("name" -> categoryNames(2))))))
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
      val body = request.body.asJson.getOrElse(Json.obj())
      val catId = (body \ "category").validate[Int].getOrElse(0)
      val subId = (body \ "subcategory").validate[Int].getOrElse(0)
      val manId = (body \ "manufacturer").validate[Int].getOrElse(0)
      val name = (body \ "name").validate[String].getOrElse("")
      val description = (body \ "description").validate[String].getOrElse("")
      val price = (body \ "price").validate[Int].getOrElse(0)
      val amount = (body \ "amount").validate[Int].getOrElse(-1)
      if(catId == 0 || subId == 0 || manId == 0 || name == "" || description == "" || price <= 0 || amount < 0){
        BadRequest(messages.invalidBody)
      }
      else{
        val catQuery = categoryRepo.getById(catId)
        val subQuery = subCategoryRepository.getById(subId)
        val manQuery = manufacturerRepo.getById(manId)
        val category = Await.result(catQuery, duration.Duration.Inf)
        val subcat = Await.result(subQuery, duration.Duration.Inf)
        val man = Await.result(manQuery, duration.Duration.Inf)
        if(category.isEmpty || subcat.isEmpty || man.isEmpty){
          BadRequest(messages.invalidBody)
        }
        else{
          val newPrd = productsRepo.create(name, description, price, amount, Option(manId), Option(catId), Option(subId))
          val product = Await.result(newPrd, duration.Duration.Inf)
          Ok(Json.toJson(product))
        }
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
      if(oldProduct.nonEmpty){
        val body = json.getOrElse(Json.obj())
        val catId = (body \ "category").validate[Int].getOrElse(0)
        val subId = (body \ "subcategory").validate[Int].getOrElse(0)
        val manId = (body \ "manufacturer").validate[Int].getOrElse(0)
        val name = (body \ "name").validate[String].getOrElse("")
        val description = (body \ "description").validate[String].getOrElse("")
        val price = (body \ "price").validate[Int].getOrElse(0)
        val amount = (body \ "amount").validate[Int].getOrElse(-1)
        val catQuery = categoryRepo.getById(catId)
        val subQuery = subCategoryRepository.getById(subId)
        val manQuery = manufacturerRepo.getById(manId)
        val category = Await.result(catQuery, duration.Duration.Inf)
        val subcat = Await.result(subQuery, duration.Duration.Inf)
        val man = Await.result(manQuery, duration.Duration.Inf)
        if(category.isEmpty || man.isEmpty || subcat.isEmpty || name == "" || description == "" || price <= 0 || amount < 0){
          BadRequest(messages.invalidBody)
        }
        else{
          val newProduct = Product(oldProduct.head._1.id, name, description, price, amount, Option(manId), Option(catId), Option(subId))
          val updateQuery = productsRepo.update(oldProduct.head._1.id, newProduct)
          Await.result(updateQuery, duration.Duration.Inf)
          Ok(Json.toJson(newProduct))
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
        val name = (body \ "name").validate[String].getOrElse("")
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
        BadRequest(messages.emptyBody)
      }
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }

  def updateManufacturerJson(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val manQuery = manufacturerRepo.getById(id)
      val oldMan = Await.result(manQuery, duration.Duration.Inf)
      val json = request.body.asJson
      if(json.nonEmpty && oldMan.nonEmpty){
        val body = json.get
        val name = (body \ "name").validate[String].getOrElse("")
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
        BadRequest(messages.emptyBody)
      }
    }
    else{
      Forbidden(messages.notAuthorized)
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
      val description = (body \ "description").validate[String].getOrElse("")
      val product = (body \ "product").validate[Int].getOrElse(0)
      if(description == "" || product == 0){
        BadRequest(messages.invalidBody)
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
          BadRequest(messages.invalidBody)
        }
      }
    }
    else{
      BadRequest(messages.emptyBody)
    }
  }

  def updateReviewJson(id: Int) = silhouette.SecuredAction { implicit request =>
    val oldRevQuery = reviewRepo.getById(id)
    val oldRev = Await.result(oldRevQuery, duration.Duration.Inf)
    val json = request.body.asJson
    if(oldRev.nonEmpty){
      if(oldRev.get.user == request.identity.id){
        val body = json.getOrElse(Json.obj())
        val description = (body \ "description").validate[String].getOrElse("")
        val productId = (body \ "product").validate[Int].getOrElse(0)
        val prdQuery = productsRepo.getById(productId)
        val prdRes = Await.result(prdQuery, duration.Duration.Inf)
        if(description == "" || prdRes.isEmpty){
          BadRequest(messages.invalidBody)
        }
        else{
          val newRev = Review(id, description, request.identity.id, productId, oldRev.get.date)
          val updateRev = reviewRepo.update(id, newRev)
          Await.result(updateRev, duration.Duration.Inf)
          Ok(Json.toJson(newRev))
        }
      }
      else{
        Forbidden(messages.emptyBody)
      }
    }
    else{
      BadRequest(Json.obj("message" -> "Empty request body or review not found"))
    }
  }

  def deleteReview(id: Int) = silhouette.SecuredAction { implicit request =>
    if(request.identity.role == "ADMIN"){
      val del = reviewRepo.delete(id)
      Await.result(del, duration.Duration.Inf)
      Ok(Json.obj("message" -> "Product deleted"))
    }
    else{
      Forbidden(messages.notAuthorized)
    }
  }

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
      Forbidden(messages.notAuthorized)
    }
  }
}
