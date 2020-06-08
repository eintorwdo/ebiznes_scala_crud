package models

import slick.lifted.TableQuery

trait AuthTables {
    val loginInfos = TableQuery[LoginInfoTable]
    val userLoginInfos = TableQuery[UserLoginInfoTable]
    val oauth2Infos = TableQuery[OAuth2InfoTable]
}