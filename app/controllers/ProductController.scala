package controllers

import models.{Category, CategoryRepository, Product, ProductRepository, ManufacturerRepository, Manufacturer, SubCategoryRepository, SubCategory, Review, ReviewRepository}
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProductController @Inject()(productsRepo: ProductRepository, categoryRepo: CategoryRepository, manufacturerRepo: ManufacturerRepository, subCategoryRepository: SubCategoryRepository, reviewRepo: ReviewRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {

  val productForm: Form[CreateProductForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> number,
      "amount" -> number,
      "manufacturer" -> number,
      "category" -> number,
      "subcategory" -> number,
    )(CreateProductForm.apply)(CreateProductForm.unapply)
  }

  val updateProductForm: Form[UpdateProductForm] = Form {
    mapping(
      "id" -> number,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> number,
      "amount" -> number,
      "manufacturer" -> number,
      "category" -> number,
      "subcategory" -> number,
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

  def products() = Action.async { implicit request: MessagesRequest[AnyContent] =>
  val produkty = productsRepo.list()
    produkty.map(prds => {
      Ok(views.html.products(prds))
    })
  }

  def product(id: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val produkt = productsRepo.getById(id.toInt)
    produkt.map(prd => {
      if(prd.isEmpty){
        Ok(views.html.index())
      }
      else{
        Ok(views.html.product(prd.head))
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
          Redirect(routes.ProductController.addProduct()).flashing("success" -> "product.created")
        }
      }
    )

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

case class CreateProductForm(name: String, description: String, price: Int, amount: Int, manufacturer: Int, category: Int, subcategory: Int)
case class UpdateProductForm(id: Int, name: String, description: String, price: Int, amount: Int, manufacturer: Int, category: Int, subcategory: Int)
case class CreateManufacturerForm(name: String)
case class UpdateManufacturerForm(id: Int, name: String)