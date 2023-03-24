package cz.dzubera.callwarden

import cz.dzubera.callwarden.utils.Config
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URL


object HttpRequest {


    fun getProjects(domain: String, user: Int, onResponse: (HttpResponse) -> Unit) {
        println("staaaaacgh")

        val url = URL(Config.BASE_URL)
        val client = OkHttpClient()

        val formBody: RequestBody = FormBody.Builder()
            .add("id_domeny", domain)
            .add("id_user", user.toString())
            .build()
        val request = Request.Builder()
            .addHeader("X-API-KEY", "123")
            .url(url)
            .post(formBody)
            .build()

        val call: okhttp3.Call = client.newCall(request)
        call.enqueue(object : Callback {


            override fun onFailure(call: okhttp3.Call, e: IOException) {
                println("failure")

                val httpResponse =
                    HttpResponse(e.localizedMessage ?: "unknown error", 0, ResponseStatus.ERROR)
                onResponse.invoke(httpResponse)

            }

            @Throws(IOException::class)
            override fun onResponse(call: okhttp3.Call, response: Response) {
                println("resposnzička " + response.code)
                if (response.code > 200) {
                    val httpResponse =
                        HttpResponse(response.message, response.code, ResponseStatus.ERROR)
                    onResponse.invoke(httpResponse)

                } else {
                    val body = response.body?.string().toString()
                    val httpResponse = HttpResponse(body)
                    if(response.body != null){
                        val projects = JSONObject(body).getProjectObject()
                        App.projectStorage.setProjects(projects)
                        projects.forEach { println(it.name) }
                    }

                    onResponse.invoke(httpResponse)
                }


            }
        })

    }

    fun sendEntries(domain: String, user: Int, data: String,onResponse: (HttpResponse) -> Unit) {
        println("staaaaacgh")

        val url = URL(Config.BASE_URL)
        val client = OkHttpClient()

        val formBody: RequestBody = FormBody.Builder()
            .add("id_domeny", domain)
            .add("id_user", user.toString())
            .add("data", data)
            .build()
        val request = Request.Builder()
            .addHeader("X-API-KEY", "123")
            .url(url)
            .post(formBody)
            .build()

        println(data)

        val call: okhttp3.Call = client.newCall(request)
        call.enqueue(object : Callback {


            override fun onFailure(call: okhttp3.Call, e: IOException) {
                println("failure")

                synchronized(HttpRequest::class.java) {
                    val httpResponse =
                        HttpResponse(e.localizedMessage ?: "unknown error", 0, ResponseStatus.ERROR)
                    onResponse.invoke(httpResponse)
                }

            }

            @Throws(IOException::class)
            override fun onResponse(call: okhttp3.Call, response: Response) {
                println("resposnzička")
                synchronized(HttpRequest::class.java) {
                    if (response.code > 200) {

                        val httpResponse =
                            HttpResponse(response.message, response.code, ResponseStatus.ERROR)
                        onResponse.invoke(httpResponse)

                    } else {
                        val httpResponse = HttpResponse(response.body.toString())
                        onResponse.invoke(httpResponse)
                    }
                }


            }
        })

    }
}