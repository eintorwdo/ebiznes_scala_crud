package models

import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info

@Singleton
class DBOAuth2InfoRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends AuthTables {
    val dbConfig = dbConfigProvider.get[JdbcProfile]

    import dbConfig._
    import profile.api._

  def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    val foundLoginInfo = loginInfos.filter(dbLoginInfo => dbLoginInfo.providerID === loginInfo.providerID &&
      dbLoginInfo.providerKey === loginInfo.providerKey).result.headOption;

    val action = foundLoginInfo.flatMap { dbLoginInfo =>
      oauth2Infos.filter(_.loginInfoId === dbLoginInfo.get.id).result.headOption.flatMap {
        case Some(o) => {
          oauth2Infos.filter(_.id === o.id).update(DBOAuth2Info(
            o.id, authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn,
            authInfo.refreshToken, dbLoginInfo.get.id
          ))
        }
        case None => {
          val id = UUID.randomUUID().toString
          oauth2Infos += DBOAuth2Info(id, authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn,
            authInfo.refreshToken, dbLoginInfo.get.id)
        }
      }
    }.transactionally

    db.run(action).map(_ => authInfo)
  }
}