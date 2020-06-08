package models

import java.util.UUID
import com.mohiva.play.silhouette.api.{LoginInfo}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DBLoginInfoRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, val userRepo: UserRepository)(implicit ec: ExecutionContext) extends AuthTables {
    val dbConfig = dbConfigProvider.get[JdbcProfile]

    import dbConfig._
    import profile.api._

    import userRepo.UserTable
    val users = TableQuery[UserTable]

    def saveUserLoginInfo(userID: String, loginInfo: LoginInfo): Future[Unit] = {
        val id = UUID.randomUUID().toString
        val dbLoginInfo = DBLoginInfo(id, loginInfo.providerID, loginInfo.providerKey)

        val actions = for {
        _ <- loginInfos += dbLoginInfo
        userLoginInfo = DBUserLoginInfo(userID, dbLoginInfo.id)
        _ <- userLoginInfos += userLoginInfo
        } yield ()
        db.run(actions)
    }

    def checkEmailIsAlreadyInUse(email: String) = db.run {
        users.filter(_.email === email).exists.result
    }

    // /**
    //     * Finds a user, login info pair by userID and login info providerID
    //     *
    //     * @param userId     user id
    //     * @param providerId provider id
    //     * @return Some(User, LoginInfo) if there is a user by userId which has login method for provider by provider ID, otherwise None
    //     */
    // def find(userId: Int, providerId: String): Future[Option[(User, LoginInfo)]] = {
    //     val action = for {
    //     ((_, li), u) <- userLoginInfos.filter(_.userID === userId)
    //         .join(loginInfos).on(_.loginInfoId === _.id)
    //         .join(users).on(_._1.userID === _.id)

    //     if li.providerID === providerId
    //     } yield (u, li)

    //     db.run(action.result.headOption).map(_.map{case (u, li) => (u, DBLoginInfo.toLoginInfo(li))})
    // }

    /**
    * Get list of user authentication methods providers
    *
    * @param email user email
    * @return
    */
    def getAuthenticationProviders(email: String): Future[Seq[String]] = {
        val action = for {
        ((_, _), li) <- users.filter(_.email === email)
            .join(userLoginInfos).on(_.id === _.userID)
            .join(loginInfos).on(_._2.loginInfoId === _.id)
        } yield li.providerID

        db.run(action.result)
    }
}