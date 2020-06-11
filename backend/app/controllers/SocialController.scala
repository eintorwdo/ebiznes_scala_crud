package controllers

import silhouette._

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.api.{LoginEvent, Silhouette}
import com.mohiva.play.silhouette.impl.providers.{CommonSocialProfileBuilder, SocialProvider, SocialProviderRegistry}
import models.DBLoginInfoRepository
import javax.inject.Inject
import models.User
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import services.UserService
import utils.DefaultEnv
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

class SocialController @Inject()(cc: MessagesControllerComponents,
                                 silhouette: Silhouette[DefaultEnv],
                                 configuration: Configuration,
                                 clock: Clock,
                                 userService: UserService,
                                 loginInfoRepo: DBLoginInfoRepository,
                                 authInfoRepository: AuthInfoRepository,
                                 socialProviderRegistry: SocialProviderRegistry)
                                (implicit ex: ExecutionContext)
  extends MessagesAbstractController(cc) {

  def authenticate(provider: String) = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) => {
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => {
            p.retrieveProfile(authInfo).flatMap { profile =>
              loginInfoRepo.getAuthenticationProviders(profile.email.get).flatMap { providers =>
                if (providers.contains(provider) || providers.isEmpty) {
                  val userToCreate = User(id = UUID.randomUUID().toString(),
                    firstname = profile.firstName,
                    lastname = profile.lastName,
                    email = profile.email.getOrElse(""),
                    password = None,
                    role = "REGULAR")

                  for {
                    user <- userService.saveOrUpdate(userToCreate, profile.loginInfo)
                    _ <- authInfoRepository.add(profile.loginInfo, authInfo)

                    authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
                    token <- silhouette.env.authenticatorService.init(authenticator)
                    tokenExpiry = authenticator.expirationDateTime.getMillis
                    result <- silhouette.env.authenticatorService.embed(
                      token, Redirect(s"http://localhost:3000/auth?token=${token}&email=${profile.email.get}&tokenExpiry=${tokenExpiry}&role=${user.role}"))
                  } yield {
                    silhouette.env.eventBus.publish(LoginEvent(user, request))
                    result
                  }
                } else {
                  val msg="Email is in use"
                  Future.successful(Redirect(s"http://localhost:3000/error?msg=${msg}"))
                }
              }
            }
          }
        }
      }
      case None => Future.successful(Status(BAD_REQUEST)(Json.obj("error" -> s"No '$provider' provider")))
    }).recover {
      case e: ProviderException => {
        val msg = "Authentication error" // Unknown error
        Redirect(s"http://localhost:3000/error?msg=${e}")
      }
    }
  }
}