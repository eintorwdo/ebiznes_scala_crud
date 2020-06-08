package models
import com.mohiva.play.silhouette.api.{LoginInfo}

case class DBLoginInfo(id: String, providerID: String, providerKey: String)