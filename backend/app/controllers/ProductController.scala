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
      "user" -> number,
      "product" -> number
    )(UpdateReviewForm.apply)(UpdateReviewForm.unapply)
  }

  val reviewForm: Form[CreateReviewForm] = Form {
    mapping(
      "description" -> nonEmptyText,
      "user" -> number,
      "product" -> number
    )(CreateReviewForm.apply)(CreateReviewForm.unapply)
  }

  def productsJson(query: Option[String]) = Action { implicit request: MessagesRequest[AnyContent] =>
    val searchQuery = query.getOrElse("")
    var dbQuery = productsRepo.list()
    if(searchQuery != ""){
      dbQuery = productsRepo.search(searchQuery)
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
        val revs = res._2.map(x => {Json.obj("id" -> x._1.id, "description" -> x._1.description, "userid" -> x._1.user, "username" -> x._2.name)})
        Ok(Json.obj("product" -> Json.obj("info" -> res._1.head._1, "manufacturer" -> res._1.head._2, "category" -> res._1.head._3, "subcategory" -> res._1.head._4),
                    "reviews" -> revs))
      }
    })
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
      val userJs = (body \ "user").validate[Int]
      val prdJs = (body \ "product").validate[Int]
      val description = dscJs.getOrElse("")
      val user = userJs.getOrElse(0)
      val product = prdJs.getOrElse(0)
      if(description == "" || user == 0 || product == 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val prdQuery = productsRepo.getById(product)
        val usrQuery = userRepo.getById(user)
        val userRes = Await.result(usrQuery, duration.Duration.Inf)
        val prdRes = Await.result(prdQuery, duration.Duration.Inf)
        if(userRes.nonEmpty && prdRes.nonEmpty){
          val createRev = reviewRepo.create(description, user, product)
          val rev = Await.result(createRev, duration.Duration.Inf)
          Ok(Json.toJson(rev))
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
      val userJs = (body \ "user").validate[Int]
      val prdJs = (body \ "product").validate[Int]
      val description = dscJs.getOrElse("")
      val user = userJs.getOrElse(0)
      val product = prdJs.getOrElse(0)
      if(description == "" || user == 0 || product == 0){
        BadRequest(Json.obj("message" -> "Invalid request body"))
      }
      else{
        val prdQuery = productsRepo.getById(product)
        val usrQuery = userRepo.getById(user)
        val userRes = Await.result(usrQuery, duration.Duration.Inf)
        val prdRes = Await.result(prdQuery, duration.Duration.Inf)
        if(userRes.nonEmpty && prdRes.nonEmpty){
          val newRev = Review(id, description, user, product)
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
        reviewRepo.create(review.description, review.user, review.product).map { _ =>
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
        val revForm = updateReviewForm.fill(UpdateReviewForm(rev.id, rev.description, rev.user, rev.product))
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
        reviewRepo.update(review.id, Review(review.id, review.description, review.user, review.product)).map { _ =>
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
case class CreateReviewForm(description: String, user: Int, product: Int)
case class UpdateReviewForm(id: Int, description: String, user: Int, product: Int)