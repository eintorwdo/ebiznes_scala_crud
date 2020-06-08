// package controllers

// import models.{User, UserRepository}

// import javax.inject.Inject
// import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
// import com.mohiva.play.silhouette.api._
// import com.mohiva.play.silhouette.api.exceptions.ProviderException
// import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
// import com.mohiva.play.silhouette.api.util.Clock
// import com.mohiva.play.silhouette.impl.providers._
// import play.api.Configuration
// import play.api.libs.json.Json
// import play.api._
// import play.api.mvc._

// import scala.concurrent.{ExecutionContext, Future}

// trait DefaultEnv extends Env {
//   type I = User
//   type A = JWTAuthenticator
// }


// /**
//   * The social auth controller.
//   *
//   * @param components             The Play controller components.
//   * @param silhouette             The Silhouette stack.
//   * @param userService            The user service implementation.
//   * @param authInfoRepository     The auth info service implementation.
//   * @param socialProviderRegistry The social provider registry.
//   * @param authenticateService    authenticate service
//   * @param ex                     The execution context.
//   */
// class SocialAuthController @Inject()(components: ControllerComponents,
//                                      silhouette: Silhouette[DefaultEnv],
//                                      configuration: Configuration,
//                                      authInfoRepository: AuthInfoRepository,
//                                      socialProviderRegistry: SocialProviderRegistry)
//                                     (implicit ex: ExecutionContext) extends BaseController {

//   /**
//     * Authenticates a user against a social provider.
//     *
//     * @param provider The ID of the provider to authenticate against.
//     * @return The result to display.
//     */
//   def authenticate(provider: String) = Action.async { implicit request: Request[AnyContent] =>
    
//   }
// }