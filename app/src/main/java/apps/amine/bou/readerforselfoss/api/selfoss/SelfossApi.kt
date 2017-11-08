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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap


class SelfossApi(c: Context, callingActivity: Activity, isWithSelfSignedCert: Boolean, shouldLog: Boolean) {

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

    fun Credentials.createAuthenticator(): DispatchingAuthenticator =
        DispatchingAuthenticator.Builder()
            .with("digest", DigestAuthenticator(this))
            .with("basic", BasicAuthenticator(this))
            .build()

    fun DispatchingAuthenticator.getHttpClien(isWithSelfSignedCert: Boolean): OkHttpClient.Builder {
        val authCache = ConcurrentHashMap<String, CachingAuthenticator>()
        return OkHttpClient
            .Builder()
            .maybeWithSelfSigned(isWithSelfSignedCert)
            .authenticator(CachingAuthenticatorDecorator(this, authCache))
            .addInterceptor(AuthenticationCacheInterceptor(authCache))
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
                .setLenient()
                .create()

        val logging = HttpLoggingInterceptor()

        logging.level = if (shouldLog)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE

        val httpClient = authenticator.getHttpClien(isWithSelfSignedCert)

        httpClient.addInterceptor(logging)

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
            Config.logoutAndRedirect(c, callingActivity, config.settings.edit(), baseUrlFail = true)
        }
    }

    fun login(): Call<SuccessResponse> =
        service.loginToSelfoss(config.userLogin, config.userPassword)

    fun readItems(tag: String?, sourceId: Long?, search: String?, itemsNumber: Int, offset: Int): Call<List<Item>> =
        getItems("read", tag, sourceId, search, itemsNumber, offset)

    fun newItems(tag: String?, sourceId: Long?, search: String?, itemsNumber: Int, offset: Int): Call<List<Item>> =
        getItems("unread", tag, sourceId, search, itemsNumber, offset)

    fun starredItems(tag: String?, sourceId: Long?, search: String?, itemsNumber: Int, offset: Int): Call<List<Item>> =
        getItems("starred", tag, sourceId, search, itemsNumber, offset)

    private fun getItems(type: String, tag: String?, sourceId: Long?, search: String?, items: Int, offset: Int): Call<List<Item>> =
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

    val sources: Call<List<Sources>>
        get() = service.sources(userName, password)

    fun deleteSource(id: String): Call<SuccessResponse> =
        service.deleteSource(id, userName, password)

    fun spouts(): Call<Map<String, Spout>> =
        service.spouts(userName, password)

    fun createSource(title: String, url: String, spout: String, tags: String, filter: String): Call<SuccessResponse> =
        service.createSource(title, url, spout, tags, filter, userName, password)

}
