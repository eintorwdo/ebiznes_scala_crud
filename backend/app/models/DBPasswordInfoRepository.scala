package models

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.api.LoginInfo
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class DBPasswordInfoRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext, val classTag: ClassTag[PasswordInfo])
  extends DelegableAuthInfoDAO[PasswordInfo] with AuthTables {

    val dbConfig = dbConfigProvider.get[JdbcProfile]

    import dbConfig._
    import profile.api._


    class PasswordInfoTable(tag: Tag) extends Table[DBPasswordInfo](tag, "password_info") {
        def hasher = column[String]("hasher")
        def password = column[String]("password")
        def salt = column[Option[String]]("salt")
        def loginInfoId = column[String]("login_info_id")
        def loginInfo_fk = foreignKey("login_info_fk", loginInfoId, TableQuery[LoginInfoTable])(_.id)

        def * = (hasher, password, salt, loginInfoId) <> ((DBPasswordInfo.apply _).tupled, DBPasswordInfo.unapply)
    }

    val passwordInfos = TableQuery[PasswordInfoTable]

    def findLoginInfo(loginInfo: LoginInfo) =
        loginInfos.filter(dbLoginInfo => dbLoginInfo.providerID === loginInfo.providerID &&
        dbLoginInfo.providerKey === loginInfo.providerKey)

    def findPassword(loginInfo: LoginInfo) = {
        passwordInfos.filter(_.loginInfoId in findLoginInfo(loginInfo).map(_.id))
    }

    def addAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
        findLoginInfo(loginInfo).result.head.flatMap { dbLoginInfo =>
        passwordInfos += DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, dbLoginInfo.id)
        }.transactionally

    def updateAction(loginInfo: LoginInfo, passwordInfo: PasswordInfo) = {
        findPassword(loginInfo)
        .map(dbPasswordInfo => (dbPasswordInfo.hasher, dbPasswordInfo.password, dbPasswordInfo.salt))
        .update((passwordInfo.hasher, passwordInfo.password, passwordInfo.salt))
    }

    def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = db.run {
        findPassword(loginInfo).result.headOption.map(pwd => pwd.map(p =>
        PasswordInfo(p.hasher, p.password, p.salt)))
    }

    def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
        // val action = loginInfos.filter(dbLoginInfo => dbLoginInfo.providerId === loginInfo.providerID &&
        //   dbLoginInfo.providerKey === loginInfo.providerKey)
        //   .result
        //   .headOption
        //   .flatMap { dbLoginInfo =>
        //     passwordInfoTable += PasswordInfoDb(authInfo.hasher, authInfo.password, authInfo.salt, dbLoginInfo.get.id)
        //   }.transactionally

        // db.run(action).map(_ => authInfo)
        db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)

    }

    def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = db.run {
        updateAction(loginInfo, authInfo).map(_ => authInfo)
    }

    def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
        val query = findLoginInfo(loginInfo)
        .joinLeft(passwordInfos).on(_.id === _.loginInfoId)
        val action = query.result.head.flatMap {
        case (_, Some(_)) => updateAction(loginInfo, authInfo)
        case (_, None) => addAction(loginInfo, authInfo)
        }
        db.run(action).map(_ => authInfo)
    }

    def remove(loginInfo: LoginInfo): Future[Unit] = db.run {
        findPassword(loginInfo).delete.map(_ => ())
    }
}