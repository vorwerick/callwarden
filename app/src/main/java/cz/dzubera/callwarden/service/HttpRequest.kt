package cz.dzubera.callwarden.service

import android.util.Log
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.BuildConfig
import cz.dzubera.callwarden.storage.getProjectObject
import cz.dzubera.callwarden.utils.Config
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit


object HttpRequest {

    const val TIMEOUT = 20000L

    fun getProjects(domain: String, user: Int, onResponse: (HttpResponse) -> Unit) {
        println("staaaaacgh")


        val url = URL(Config.PROJECTS_URL)
        val client = OkHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true).build()

        val formBody: RequestBody =
            FormBody.Builder().add("id_domeny", domain).add("id_user", user.toString()).build()
        val request =
            Request.Builder().addHeader("X-API-KEY", getApiKey(domain).toString()).url(url)
                .post(formBody).build()

        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {


            override fun onFailure(call: Call, e: IOException) {
                println("failure")

                val httpResponse =
                    HttpResponse(e.localizedMessage ?: "unknown error", 0, ResponseStatus.ERROR)
                onResponse.invoke(httpResponse)

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                println("resposnzička " + response.code)
                if (response.code > 200) {
                    val httpResponse =
                        HttpResponse(response.message, response.code, ResponseStatus.ERROR)
                    onResponse.invoke(httpResponse)

                } else {
                    val body = response.body?.string().toString()
                    val httpResponse = HttpResponse(body)
                    val projects = try {
                        JSONObject(body).getProjectObject()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                    projects?.let {
                        App.projectStorage.setProjects(projects)
                        projects.forEach { println(it.name) }

                        onResponse.invoke(httpResponse)
                    }
                }


            }
        })

    }

    fun sendVersion(
        domain: String, user: Int, onResponse: (VersionState) -> Unit
    ) {
        Log.i("Http Request Service", "Send")


        val url = URL(Config.SEND_VERSION)
        val client = OkHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true).build()

        val formBody: RequestBody =
            FormBody.Builder().add("id_domeny", domain).add("id_user", user.toString())
                .add("verze", BuildConfig.VERSION_NAME)
              //  .add("cislo_buildu", BuildConfig.VERSION_CODE.toString())
                .build()




        val request =
            Request.Builder().addHeader("X-API-KEY", getApiKey(domain)).url(url).post(formBody)
                .build()

        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {


            override fun onFailure(call: Call, e: IOException) {
                Log.e(
                    "Http Request Service", "Version ${e.localizedMessage ?: "unknown error"}"
                )

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                if (response.code > 200) {


                    Log.e(
                        "Http Request Service", "Version ${response.code} -> ${response.message}"
                    )
                } else {

                    val body = response.body?.string()
                    Log.i("Http Request Service", "Version: $body")
                    body?.let {
                        val versionState = try {
                            JSONObject(body)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        versionState?.let {
                            onResponse.invoke(
                                VersionState(
                                    versionState.optInt("code"), versionState.optString("url")
                                )
                            )
                        }
                    }


                }


            }
        })

    }

    data class VersionState(val state: Int?, val url: String?) {

        fun getState(): States {
            return when (state) {
                0 -> States.OK
                1 -> States.NEW_VERSION
                2 -> States.UPDATE_NEEDED
                else -> States.UNKNOWN
            }
        }

        enum class States {
            OK, NEW_VERSION, UPDATE_NEEDED, UNKNOWN
        }
    }

    suspend fun sendIncomingCall(
        domain: String,
        user: Int,
        projectId: String,
        number: String,
        token: String,
        onResponse: (HttpResponse) -> Unit
    ) {


        val url = URL(Config.SEND_INCOMING_CALL_URL)
        val client = OkHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true).build()

        val formBody: RequestBody =
            FormBody.Builder().add("id_domeny", domain).add("id_user", user.toString())
                .add("projectId", projectId).add("number", number).add("firebase_token", token)
                .build()
        val request =
            Request.Builder().addHeader("X-API-KEY", getApiKey(domain)).url(url).post(formBody)
                .build()

        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {


            override fun onFailure(call: Call, e: IOException) {
                Log.e(
                    "Http Request Service",
                    "Incoming number $number not sent via error! ${e.localizedMessage ?: "unknown error"}"
                )

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {

                if (response.code > 200) {

                    Log.e(
                        "Http Request Service",
                        "Incoming number $number not sent! ${response.code} -> ${response.message}"
                    )
                } else {
                    Log.i("Http Request Service", "Incoming number $number sent!")
                }


            }
        })

    }

//     fun sendToken(
//        domain: String,
//        user: Int,
//        token: String,
//        onResponse: (HttpResponse) -> Unit = {}
//    ) {
//
//
//        val url = URL(Config.SEND_FIREBASE_TOKEN)
//        val client = OkHttpClient().newBuilder()
//            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
//            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
//            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
//            .retryOnConnectionFailure(true)
//            .build()
//
//        val formBody: RequestBody = FormBody.Builder()
//            .add("firebase_token", token)
//            .add("id_user", user.toString())
//            .add("id_domeny", domain)
//            .build()
//        val request = Request.Builder()
//            .addHeader("X-API-KEY", getApiKey(domain))
//            .url(url)
//            .post(formBody)
//            .build()
//
//        val call: Call = client.newCall(request)
//        call.enqueue(object : Callback {
//
//
//            override fun onFailure(call: Call, e: IOException) {
//                onResponse.invoke(HttpResponse(null, 0, ResponseStatus.ERROR))
//
//                Log.e(
//                    "Http Request Service",
//                    "Sending firebase token failed"
//                )
//
//            }
//
//            @Throws(IOException::class)
//            override fun onResponse(call: Call, response: Response) {
//
//                if (response.code > 200) {
//                    onResponse.invoke(HttpResponse(null, response.code, ResponseStatus.ERROR))
//
//                    Log.e(
//                        "Http Request Service",
//                        "Sending firebase token failed"
//                    )
//                } else {
//                    val body = response.body?.string().toString()
//                    onResponse.invoke(HttpResponse(body))
//                    Log.i("Http Request Service", "Firebase token sent!")
//                }
//
//
//            }
//        })
//
//    }


    fun getApiKey(domain: String): String {
        val date = SimpleDateFormat("d.M.yyyy").format(Date(System.currentTimeMillis()))
        val str = domain + date
        val digest = MessageDigest.getInstance("SHA-256").apply { reset() }
        val byteData: ByteArray = digest.digest(str.toByteArray())
        val result = StringBuffer().apply {
            byteData.forEach {
                append(((it.toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
        }.toString()
        println("APIK: " + result)
        return result
    }

    fun sendEntries(domain: String, user: Int, data: String, onResponse: (HttpResponse) -> Unit) {


        val url = URL(Config.CALL_URL)
        val client = OkHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS).retryOnConnectionFailure(true).build()

        val formBody: RequestBody =
            FormBody.Builder().add("id_domeny", domain).add("id_user", user.toString())
                .add("data", data).build()
        val request =
            Request.Builder().addHeader("X-API-KEY", getApiKey(domain)).url(url).post(formBody)
                .build()

        println(data)

        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {


            override fun onFailure(call: Call, e: IOException) {
                println("failure")

                synchronized(HttpRequest::class.java) {
                    val httpResponse =
                        HttpResponse(e.localizedMessage ?: "unknown error", 0, ResponseStatus.ERROR)
                    onResponse.invoke(httpResponse)
                }

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                println("resposnzička")
                synchronized(HttpRequest::class.java) {
                    if (response.code > 200) {

                        val httpResponse =
                            HttpResponse(response.message, response.code, ResponseStatus.ERROR)
                        onResponse.invoke(httpResponse)

                    } else {
                        val body = response.body?.string() ?: ""
                        val httpResponse = HttpResponse(body)
                        onResponse.invoke(httpResponse)
                    }
                }


            }
        })

    }
}
