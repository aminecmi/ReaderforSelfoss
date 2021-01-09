package apps.amine.bou.readerforselfoss.api.selfoss

import android.app.Activity
import android.content.Context
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.getUnsafeHttpClient
import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.Credentials
import com.burgstaller.okhttp.digest.DigestAuthenticator
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SelfossApi(
    c: Context,
    callingActivity: Activity?,
    isWithSelfSignedCert: Boolean,
    timeout: Long
) {

    private lateinit var service: SelfossService
    private val config: Config = Config(c)
    private val userName: String
    private val password: String

    fun OkHttpClient.Builder.maybeWithSelfSigned(isWithSelfSignedCert: Boolean): OkHttpClient.Builder =
        if (isWithSelfSignedCert) {
            getUnsafeHttpClient()
        } else {
            this
        }

    fun OkHttpClient.Builder.maybeWithSettingsTimeout(timeout: Long): OkHttpClient.Builder =
        if (timeout != -1L) {
            this.readTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(timeout, TimeUnit.SECONDS)
        } else {
            this
        }

    fun Credentials.createAuthenticator(): DispatchingAuthenticator =
        DispatchingAuthenticator.Builder()
            .with("digest", DigestAuthenticator(this))
            .with("basic", BasicAuthenticator(this))
            .build()

    fun DispatchingAuthenticator.getHttpClien(isWithSelfSignedCert: Boolean, timeout: Long): OkHttpClient.Builder {
        val authCache = ConcurrentHashMap<String, CachingAuthenticator>()
        return OkHttpClient
            .Builder()
            .maybeWithSettingsTimeout(timeout)
            .maybeWithSelfSigned(isWithSelfSignedCert)
            .authenticator(CachingAuthenticatorDecorator(this, authCache))
            .addInterceptor(AuthenticationCacheInterceptor(authCache))
            .addInterceptor(object: Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request: Request = chain.request()
                    val response: Response = chain.proceed(request)

                    if (response.code() == 408) {
                        return response
                    }
                    return response
                }
            })
    }

    init {
        userName = config.userLogin
        password = config.userPassword

        val authenticator =
            Credentials(
                config.httpUserLogin,
                config.httpUserPassword
            ).createAuthenticator()

        val gson =
            GsonBuilder()
                .registerTypeAdapter(Boolean::class.javaPrimitiveType, BooleanTypeAdapter())
                .registerTypeAdapter(SelfossTagType::class.java, SelfossTagTypeTypeAdapter())
                .setLenient()
                .create()

        val logging = HttpLoggingInterceptor()


        logging.level = HttpLoggingInterceptor.Level.NONE
        val httpClient = authenticator.getHttpClien(isWithSelfSignedCert, timeout)

        val timeoutCode = 504
        httpClient
                .addInterceptor { chain ->
                    val res = chain.proceed(chain.request())
                    if (res.code() == timeoutCode) {
                        throw SocketTimeoutException("timeout")
                    }
                    res
                }
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request()
                    try {
                        chain.proceed(request)
                    } catch (e: SocketTimeoutException) {
                        Response.Builder()
                                .code(timeoutCode)
                                .protocol(Protocol.HTTP_2)
                                .body(ResponseBody.create(MediaType.parse("text/plain"), ""))
                                .message("")
                                .request(request)
                                .build()
                    }
                }

        try {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(config.baseUrl)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            service = retrofit.create(SelfossService::class.java)
        } catch (e: IllegalArgumentException) {
            if (callingActivity != null) {
                Config.logoutAndRedirect(c, callingActivity, config.settings.edit(), baseUrlFail = true)
            }
        }
    }

    fun login(): Call<SuccessResponse> =
        service.loginToSelfoss(config.userLogin, config.userPassword)

    fun readItems(
        tag: String?,
        sourceId: Long?,
        search: String?,
        itemsNumber: Int,
        offset: Int
    ): Call<List<Item>> =
        getItems("read", tag, sourceId, search, itemsNumber, offset)

    fun newItems(
        tag: String?,
        sourceId: Long?,
        search: String?,
        itemsNumber: Int,
        offset: Int
    ): Call<List<Item>> =
        getItems("unread", tag, sourceId, search, itemsNumber, offset)

    fun starredItems(
        tag: String?,
        sourceId: Long?,
        search: String?,
        itemsNumber: Int,
        offset: Int
    ): Call<List<Item>> =
        getItems("starred", tag, sourceId, search, itemsNumber, offset)

    fun allItems(): Call<List<Item>> =
        service.allItems(userName, password)

    fun allNewItems(): Call<List<Item>> =
            service.allTypeItems("unread", userName, password)

    fun allReadItems(): Call<List<Item>> =
            service.allTypeItems("read", userName, password)

    fun allStarredItems(): Call<List<Item>> =
            service.allTypeItems("starred", userName, password)

    private fun getItems(
        type: String,
        tag: String?,
        sourceId: Long?,
        search: String?,
        items: Int,
        offset: Int
    ): Call<List<Item>> =
        service.getItems(type, tag, sourceId, search, userName, password, items, offset)

    fun markItem(itemId: String): Call<SuccessResponse> =
        service.markAsRead(itemId, userName, password)

    fun unmarkItem(itemId: String): Call<SuccessResponse> =
        service.unmarkAsRead(itemId, userName, password)

    fun readAll(ids: List<String>): Call<SuccessResponse> =
        service.markAllAsRead(ids, userName, password)

    fun starrItem(itemId: String): Call<SuccessResponse> =
        service.starr(itemId, userName, password)

    fun unstarrItem(itemId: String): Call<SuccessResponse> =
        service.unstarr(itemId, userName, password)

    val stats: Call<Stats>
        get() = service.stats(userName, password)

    val tags: Call<List<Tag>>
        get() = service.tags(userName, password)

    fun update(): Call<String> =
        service.update(userName, password)

    val sources: Call<List<Source>>
        get() = service.sources(userName, password)

    fun deleteSource(id: String): Call<SuccessResponse> =
        service.deleteSource(id, userName, password)

    fun spouts(): Call<Map<String, Spout>> =
        service.spouts(userName, password)

    fun createSource(
        title: String,
        url: String,
        spout: String,
        tags: String,
        filter: String
    ): Call<SuccessResponse> =
        service.createSource(title, url, spout, tags, filter, userName, password)
}
