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

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProductController @Inject()(productsRepo: ProductRepository, categoryRepo: CategoryRepository, manufacturerRepo: ManufacturerRepository, subCategoryRepository: SubCategoryRepository, reviewRepo: ReviewRepository, userRepo: UserRepository, orderDetailRepo: OrderDetailRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

  val productForm: Form[CreateProductForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> number,
      "amount" -> number,
      "manufacturer" -> optional(number),
      "category" -> optional(number),
      "subcategory" -> optional(number),
    )(CreateProductForm.apply)(CreateProductForm.unapply)
  }

  val updateProductForm: Form[UpdateProductForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> number,
      "amount" -> number,
      "manufacturer" -> optional(number),
      "category" -> optional(number),
      "subcategory" -> optional(number),
    )(UpdateProductForm.apply)(UpdateProductForm.unapply)
  }

  val updateManufacturerForm: Form[UpdateManufacturerForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText
    )(UpdateManufacturerForm.apply)(UpdateManufacturerForm.unapply)
  }

  val manufacturerForm: Form[CreateManufacturerForm] = Form {
    mapping(
      "name" -> nonEmptyText
    )(CreateManufacturerForm.apply)(CreateManufacturerForm.unapply)
  }

  val updateReviewForm: Form[UpdateReviewForm] = Form {
    mapping(
      "id" -> number,
      "description" -> nonEmptyText,
      "user" -> nonEmptyText,
      "product" -> number,
      "date" -> nonEmptyText
    )(UpdateReviewForm.apply)(UpdateReviewForm.unapply)
  }

  val reviewForm: Form[CreateReviewForm] = Form {
    mapping(
      "description" -> nonEmptyText,
      "user" -> nonEmptyText,
      "product" -> number
    )(CreateReviewForm.apply)(CreateReviewForm.unapply)
  }

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
        val revs = res._2.map(x => {Json.obj("id" -> x._1.id, "description" -> x._1.description, "userid" -> x._1.user, "username" -> x._2.firstname, "date" -> x._1.date)})
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

  def addProductJson() = Action { implicit request: MessagesRequest[AnyContent] =>
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

  def updateProductJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
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

  def products() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val produkty = productsRepo.list()
    produkty.map(prds => {
      Ok(views.html.products(prds))
    })
  }

  def product(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val produkt = productsRepo.getById(id.toInt)
    val reviews = reviewRepo.listByProdId(id.toInt)
    val users = userRepo.list()
    val result = for {
          r1 <- produkt
          r2 <- reviews
          r3 <- users
    } yield (r1, r2, r3)

    result.map(res => {
      if(res._1.isEmpty){
        BadRequest(views.html.index("Product not found"))
      }
      else{
        Ok(views.html.product(res._1.head, reviewForm, res._2, res._3))
      }
    })
  }

  def updateProduct(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val categories = categoryRepo.list()
    val subcategories = subCategoryRepository.list()
    val manufacturers = manufacturerRepo.list()
    val produkt = productsRepo.getById(id)
    val result = for {
          r1 <- categories
          r2 <- subcategories
          r3 <- manufacturers
          r4 <- produkt
    } yield (r1, r2, r3, r4)
    
    result.map(x => {
      if(x._4.isEmpty){
        BadRequest(views.html.index("Product not found"))
      }
      else{
        val prodForm = updateProductForm.fill(UpdateProductForm(x._4.head._1.id, x._4.head._1.name, x._4.head._1.description, x._4.head._1.price, x._4.head._1.amount, x._4.head._1.manufacturer, x._4.head._1.category, x._4.head._1.subcategory))
        Ok(views.html.productupdate(prodForm, (x._1, x._2, x._3)))
      }
    })
  }

  def updateProductHandle = Action.async { implicit request =>
    val categories = categoryRepo.list()
    val subcategories = subCategoryRepository.list()
    val manufacturers = manufacturerRepo.list()
    var result = for {
          r1 <- categories
          r2 <- subcategories
          r3 <- manufacturers
    } yield (r1, r2, r3)

    var res:(Seq[Category],Seq[SubCategory],Seq[Manufacturer]) = (Seq[Category](),Seq[SubCategory](),Seq[Manufacturer]())

    result.onComplete{
      case Success(r) => res = r
      case Failure(_) => print("fail")
    }

    updateProductForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.productupdate(errorForm, res))
        )
      },
      product => {
        productsRepo.update(product.id, Product(product.id, product.name, product.description, product.price, product.amount, product.manufacturer, product.category, product.subcategory)).map { _ =>
          Redirect(routes.ProductController.updateProduct(product.id)).flashing("success" -> "product updated")
        }
      }
    )

  }

  def addProduct() = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val categories = categoryRepo.list()
    val subcategories = subCategoryRepository.list()
    val manufacturers = manufacturerRepo.list()
    val result = for {
          r1 <- categories
          r2 <- subcategories
          r3 <- manufacturers
    } yield (r1, r2, r3)
    
    result.map(x => {
      Ok(views.html.productadd(productForm, x))
    })
  }

  def addProductHandle = Action.async { implicit request =>
    val categories = categoryRepo.list()
    val subcategories = subCategoryRepository.list()
    val manufacturers = manufacturerRepo.list()
    var result = for {
          r1 <- categories
          r2 <- subcategories
          r3 <- manufacturers
    } yield (r1, r2, r3)

    var res:(Seq[Category],Seq[SubCategory],Seq[Manufacturer]) = (Seq[Category](),Seq[SubCategory](),Seq[Manufacturer]())

    result.onComplete{
      case Success(r) => res = r
      case Failure(_) => print("fail")
    }

    productForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.productadd(errorForm, res))
        )
      },
      product => {
        productsRepo.create(product.name, product.description, product.price, product.amount, product.manufacturer, product.category, product.subcategory).map { _ =>
          Redirect(routes.ProductController.addProduct()).flashing("success" -> "product created")
        }
      }
    )

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

  def addManufacturerJson() = Action { implicit request: MessagesRequest[AnyContent] =>
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

  def updateManufacturerJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
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

  def addManufacturer() = Action { implicit request: MessagesRequest[AnyContent] =>
      Ok(views.html.manufactureradd(manufacturerForm))
  }

  def addManufacturerHandle = Action.async { implicit request =>
    manufacturerForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.manufactureradd(errorForm))
        )
      },
      manufacturer => {
        manufacturerRepo.create(manufacturer.name).map { _ =>
          Redirect(routes.ProductController.addManufacturer()).flashing("success" -> "manufacturer created")
        }
      }
    )
  }

  def updateManufacturer(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val manufacturer = manufacturerRepo.getById(id)
    manufacturer.map(x => {
      if(x.nonEmpty){
        val man = x.get
        val manForm = updateManufacturerForm.fill(UpdateManufacturerForm(man.id, man.name))
        Ok(views.html.manufacturerupdate(manForm))
      }
      else{
        BadRequest(views.html.index("Manufacturer not found"))
      }
    })
  }

  def updateManufacturerHandle = Action.async { implicit request =>
    updateManufacturerForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.manufacturerupdate(errorForm))
        )
      },
      manufacturer => {
        manufacturerRepo.update(manufacturer.id, Manufacturer(manufacturer.id, manufacturer.name)).map { _ =>
          Redirect(routes.ProductController.updateManufacturer(manufacturer.id)).flashing("success" -> "manufacturer updated")
        }
      }
    )
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

  def addReviewJson() = Action { implicit request: MessagesRequest[AnyContent] =>
    val json = request.body.asJson
    if(json.nonEmpty){
      val body = json.get
      val dscJs = (body \ "description").validate[String]
      val userJs = (body \ "user").validate[String]
      val prdJs = (body \ "product").validate[Int]
      val description = dscJs.getOrElse("")
      val user = userJs.getOrElse("")
      val product = prdJs.getOrElse(0)
      if(description == "" || user == "" || product == 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val prdQuery = productsRepo.getById(product)
        val usrQuery = userRepo.getById(user)
        val userRes = Await.result(usrQuery, duration.Duration.Inf)
        val prdRes = Await.result(prdQuery, duration.Duration.Inf)
        if(userRes.nonEmpty && prdRes.nonEmpty){
          val date = java.time.LocalDate.now.toString
          val createRev = reviewRepo.create(description, user, product, date)
          val rev = Await.result(createRev, duration.Duration.Inf)
          Ok(Json.obj("username" -> userRes.get.firstname, "id" -> rev.id, "date" -> rev.date, "description" -> rev.description))
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

  def updateReviewJson(id: Int) = Action { implicit request: MessagesRequest[AnyContent] =>
    val oldRevQuery = reviewRepo.getById(id)
    val oldRev = Await.result(oldRevQuery, duration.Duration.Inf)
    val json = request.body.asJson
    if(json.nonEmpty && oldRev.nonEmpty){
      val body = json.get
      val dscJs = (body \ "description").validate[String]
      val userJs = (body \ "user").validate[String]
      val prdJs = (body \ "product").validate[Int]
      val description = dscJs.getOrElse("")
      val user = userJs.getOrElse("")
      val product = prdJs.getOrElse(0)
      if(description == "" || user == "" || product == 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val prdQuery = productsRepo.getById(product)
        val usrQuery = userRepo.getById(user)
        val userRes = Await.result(usrQuery, duration.Duration.Inf)
        val prdRes = Await.result(prdQuery, duration.Duration.Inf)
        if(userRes.nonEmpty && prdRes.nonEmpty){
          val newRev = Review(id, description, user, product, oldRev.get.date)
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
      BadRequest(Json.obj("message" -> "Empty request body or review not found"))
    }
  }

  def addReviewHandle(id: Int) = Action.async { implicit request =>  //review form is integrated with product page
    val produkt = productsRepo.getById(id)
    val reviews = reviewRepo.listByProdId(id)
    val users = userRepo.list()
    val result = for {
          r1 <- produkt
          r2 <- reviews
          r3 <- users
    } yield (r1, r2, r3)
    
    var res:(Seq[(Product, Option[Manufacturer], Option[Category], Option[SubCategory])], Seq[(Review, User)], Seq[User]) = (Seq[(Product, Option[Manufacturer], Option[Category], Option[SubCategory])](), Seq[(Review, User)](), Seq[User]())

    result.onComplete{
      case Success(r) => res = r
      case Failure(_) => print("fail")
    }
    
    reviewForm.bindFromRequest.fold(
      errorForm => {
        if(!res._1.isEmpty){
          Future.successful(
            BadRequest(views.html.product(res._1.head, errorForm, res._2, res._3))
          )
        }
        else{
          Future.successful(
            Ok(views.html.index("Product not found"))
          )
        }
      },
      review => {
        val date = java.time.LocalDate.now.toString
        reviewRepo.create(review.description, review.user, review.product, date).map { _ =>
          Redirect(routes.ProductController.product(review.product)).flashing("success" -> "review created")
        }
      }
    )

  }

  def updateReview(id: Int) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val review = reviewRepo.getById(id)
    review.map(x => {
      if(x.nonEmpty){
        val rev = x.get
        val date = java.time.LocalDate.now.toString
        val revForm = updateReviewForm.fill(UpdateReviewForm(rev.id, rev.description, rev.user, rev.product, date))
        Ok(views.html.reviewupdate(revForm))
      }
      else{
        BadRequest(views.html.index("Review not found"))
      }
    })
  }

  def updateReviewHandle = Action.async { implicit request =>
    updateReviewForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(
          BadRequest(views.html.reviewupdate(errorForm))
        )
      },
      review => {
        reviewRepo.update(review.id, Review(review.id, review.description, review.user, review.product, review.date)).map { _ =>
          Redirect(routes.ProductController.product(review.product)).flashing("success" -> "review updated")
        }
      }
    )

  }

  def deleteReview(id: Int) = Action { implicit request =>
    val del = reviewRepo.delete(id)
    Await.result(del, duration.Duration.Inf)
    Ok(views.html.index("Review deleted"))
  }

  def deleteProduct(id: Int) = Action { implicit request =>
    val delRev = reviewRepo.deleteByProductId(id)
    Await.result(delRev, duration.Duration.Inf)
    val updateOrd = orderDetailRepo.deleteProductId(id)
    Await.result(updateOrd, duration.Duration.Inf)
    val delPrd = productsRepo.delete(id)
    Await.result(delPrd, duration.Duration.Inf)
    Ok(views.html.index("Product deleted"))
  }

  def deleteManufacturer(id: Int) = Action { implicit request =>
    val updateMan = productsRepo.deleteManufacturerId(id)
    Await.result(updateMan, duration.Duration.Inf)
    val del = manufacturerRepo.delete(id)
    Await.result(del, duration.Duration.Inf)
    Ok(views.html.index("Manufacturer deleted"))
  }

  def manufacturers() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val manufacturers = manufacturerRepo.list()
    manufacturers.map(cat => {
      Ok(s"Producenci: $cat")
    })
  }

  def manufacturer(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val manu = manufacturerRepo.getById(id.toInt)
    manu.map(cat => {
         Ok(s"Producent: $cat")
    })
  }

  def reviews() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val revs = reviewRepo.list()
    revs.map(cat => {
      Ok(s"Opinie: $cat")
    })
  }

  def review(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val rev = reviewRepo.getById(id.toInt)
    rev.map(cat => {
         Ok(s"Opinia: $cat")
    })
  }

  def addToCart(id: String) = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(s"Dodales do koszyka: $id")
  }

  def removeFromCart(id: String) = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(s"Usunales z koszyka: $id")
  }

  def cart() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(s"Koszyk")
  }

  def checkout() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(s"Zakupiono cos. Teraz zaplac")
  }

}

case class CreateProductForm(name: String, description: String, price: Int, amount: Int, manufacturer: Option[Int], category: Option[Int], subcategory: Option[Int])
case class UpdateProductForm(id: Int, name: String, description: String, price: Int, amount: Int, manufacturer: Option[Int], category: Option[Int], subcategory: Option[Int])
case class CreateManufacturerForm(name: String)
case class UpdateManufacturerForm(id: Int, name: String)
case class CreateReviewForm(description: String, user: String, product: Int)
case class UpdateReviewForm(id: Int, description: String, user: String, product: Int, date: String)