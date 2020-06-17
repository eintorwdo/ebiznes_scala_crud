package utils
import play.api.libs.json._

class ResponseMsgs {
    val catNotFound = Json.obj("message" -> "Category not found")
    val emptyBody = Json.obj("message" -> "Empty body")
    val notAuthorized = Json.obj("message" -> "Not authorized")
    val invalidBody = Json.obj("message" -> "Invalid request body")
}