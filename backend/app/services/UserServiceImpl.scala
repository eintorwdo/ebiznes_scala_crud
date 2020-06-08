package services

import com.mohiva.play.silhouette.api.LoginInfo
import models.{UserRepository, DBLoginInfoRepository}
import javax.inject.Inject
import models.User

import scala.concurrent.{ExecutionContext, Future}

class UserServiceImpl @Inject()(userRepo: UserRepository,
                                logiInfoRepo: DBLoginInfoRepository)
                               (implicit ec: ExecutionContext) extends UserService {

  override def retrieve(loginInfo: LoginInfo) = {
    userRepo.find(loginInfo)
  }

  def saveOrUpdate(user: User, loginInfo: LoginInfo) = {
    userRepo.find(loginInfo).flatMap {
      case Some(foundUser) => userRepo.updateNoId(user.copy(role = foundUser.role))
      case None => save(user, loginInfo)
    }
  }

  def save(user: User, loginInfo: LoginInfo) = {
    for {
      savedUser <- userRepo.save(user)
      _ <- logiInfoRepo.saveUserLoginInfo(savedUser.id, loginInfo)
    } yield savedUser
  }
}