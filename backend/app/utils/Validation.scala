package utils

class Validation {
    def areDetailsValid(details: Seq[(Int, Int)]): Boolean = {
        var areValid = true
        details.foreach(el => {
            if(el._1 <= 0 || el._2 <= 0){
                areValid = false
            }
        })
        return areValid
    }
}