package controllers

import java.util.UUID

import silhouette._

import services.UserService
import javax.inject.Inject
// import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent, Silhouette}
import com.mohiva.play.silhouette.impl.providers.{CredentialsProvider, SocialProviderRegistry}
// import com.mohiva.play.silhouette.api.services.AvatarService
import play.api.i18n.I18nSupport
// import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import utils.DefaultEnv
import models.User
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import models.DBLoginInfoRepository

class SignUpController @Inject()(components: MessagesControllerComponents,
                                 silhouette: Silhouette[DefaultEnv],
                                 userService: UserService,
                                 authInfoRepository: AuthInfoRepository,
                                 loginInfoRepo: DBLoginInfoRepository,
                                 passwordHasherRegistry: PasswordHasherRegistry,
                                 socialProviderRegistry: SocialProviderRegistry
                                 )(implicit ex: ExecutionContext) extends MessagesAbstractController(components) with I18nSupport {
    
    val signUpform: Form[CredentialsSingUpData] = Form{
        mapping(
        "firstName" -> optional(text),
        "lastName" -> optional(text),
        "email" -> email,
        "password" -> nonEmptyText
        )(CredentialsSingUpData.apply)(CredentialsSingUpData.unapply)
    }
  
    def signUp() = Action { implicit request =>
        Ok(views.html.signup(signUpform))
    }

    def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
        signUpform.bindFromRequest.fold(
        _ => Future.successful(BadRequest),
        data => {
            val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
            loginInfoRepo.checkEmailIsAlreadyInUse(data.email).flatMap(isInUse => {
            if(isInUse) Future(Status(CONFLICT)("Email is in use"))
            else {
                val userToCreate = User(id = UUID.randomUUID().toString(), email = data.email, firstname = data.firstName,
                lastname = data.lastName, role = "REGULAR", password = None)
                for {
                user <- userService.saveOrUpdate(userToCreate, loginInfo)
                authInfo = passwordHasherRegistry.current.hash(data.password)
                _ <- authInfoRepository.add(loginInfo, authInfo)

                authenticator <- silhouette.env.authenticatorService.create(loginInfo)
                token <- silhouette.env.authenticatorService.init(authenticator)
                result <- silhouette.env.authenticatorService.embed(
                    token, Ok(Json.obj(
                    "token" -> token,
                    "email" -> data.email,
                    "tokenExpiry" -> authenticator.expirationDateTime.getMillis()
                    )))
                } yield {
                silhouette.env.eventBus.publish(SignUpEvent(user, request))
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                result
                }
            }
            })
        }
        )
    }
}

case class CredentialsSingUpData(firstName: Option[String], lastName: Option[String], email: String, password: String)