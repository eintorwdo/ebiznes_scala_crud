package controllers

import models.User
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import play.api.mvc._
import services.UserService
import utils.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignInController @Inject()(cc: MessagesControllerComponents,
                                 silhouette: Silhouette[DefaultEnv],
                                 userService: UserService,
                                 credentialsProvider: CredentialsProvider)
                                (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) with I18nSupport {

  def submit() = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
        val json = request.body.asJson
        if(json.nonEmpty){
            val body = json.get
            val email = (body \ "email").validate[String]
            val password = (body \ "password").validate[String]
            val data = SignInData(email.getOrElse(""), password.getOrElse(""))
            if(data.email == "" || data.password == ""){
                Future(BadRequest(Json.obj("message" -> "Empty email or password")))
            }
            else{
                val credentials = Credentials(data.email, data.password)
                credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
                userService.retrieve(loginInfo).map {
                    case Some(user) => Success(user)
                    case None => UserNotFound
                }}.recoverWith {
                case _: InvalidPasswordException => Future.successful(InvalidPassword)
                case _: IdentityNotFoundException => Future.successful(UserNotFound)
                case e => Future.failed(e)
                }.flatMap {
                case Success(user) => {
                    val loginInfo = LoginInfo(CredentialsProvider.ID, user.email)
                    for {
                    authenticator <- silhouette.env.authenticatorService.create(loginInfo)
                    token <- silhouette.env.authenticatorService.init(authenticator)
                    result <- silhouette.env.authenticatorService.embed(token, Ok(
                        Json.obj(
                        "token" -> token,
                        "tokenExpiry" -> authenticator.expirationDateTime.getMillis,
                        "email" -> user.email,
                        "role" -> user.role
                        )))
                    } yield {
                    silhouette.env.eventBus.publish(LoginEvent(user, request))
                    result
                    }
                }
                case InvalidPassword =>
                    Future.successful(BadRequest(Json.obj("message" -> "invalid email/password")))
                case UserNotFound =>
                    Future.successful(BadRequest(Json.obj("message" -> "invalid email/password")))
                }
            }
       }
       else{
           Future(BadRequest(Json.obj("message" -> "Empty request body")))
       }
  }

  case class SignInData(email: String, password: String)
  sealed trait AuthenticateResult
  object InvalidPassword extends AuthenticateResult
  object UserNotFound extends AuthenticateResult
  case class Success(user: User) extends AuthenticateResult
}