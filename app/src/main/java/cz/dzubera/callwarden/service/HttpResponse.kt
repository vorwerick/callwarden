package cz.dzubera.callwarden.service

enum class ResponseStatus{
    SUCCESS, ERROR
}

class HttpResponse(val data: String? ,val code: Int = 200, val status: ResponseStatus = ResponseStatus.SUCCESS) {

}