package apps.amine.bou.readerforselfoss.api.selfoss

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.widget.Toast
import apps.amine.bou.readerforselfoss.LoginActivity
import apps.amine.bou.readerforselfoss.R
import java.util.concurrent.ConcurrentHashMap

import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.Credentials
import com.burgstaller.okhttp.digest.DigestAuthenticator
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import apps.amine.bou.readerforselfoss.utils.Config
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*


// codebeat:disable[ARITY,TOO_MANY_FUNCTIONS]
class SelfossApi(c: Context, callingActivity: Activity, isWithSelfSignedCert: Boolean) {

    private lateinit var service: SelfossService
    private val config: Config = Config(c)
    private val userName: String
    private val password: String

    fun OkHttpClient.Builder.maybeWithSelfSigned(isWithSelfSignedCert: Boolean): OkHttpClient.Builder =
        if (isWithSelfSignedCert) {
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> =
                        arrayOf()

                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                    }

                })

                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())

                val sslSocketFactory = sslContext.socketFactory

                OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }

            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        } else {
            this
        }

    fun Credentials.createAuthenticator(): DispatchingAuthenticator =
        DispatchingAuthenticator.Builder()
            .with("digest", DigestAuthenticator(this))
            .with("basic", BasicAuthenticator(this))
            .build()

    fun DispatchingAuthenticator.getHttpClien(isWithSelfSignedCert: Boolean): OkHttpClient {
        val authCache = ConcurrentHashMap<String, CachingAuthenticator>()
        return OkHttpClient
            .Builder()
            .maybeWithSelfSigned(isWithSelfSignedCert)
            .authenticator(CachingAuthenticatorDecorator(this, authCache))
            .addInterceptor(AuthenticationCacheInterceptor(authCache))
            .build()
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


        try {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(config.baseUrl)
                    .client(authenticator.getHttpClien(isWithSelfSignedCert))
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            service = retrofit.create(SelfossService::class.java)
        } catch (e: IllegalArgumentException) {
            Config.logoutAndRedirect(c, callingActivity, config.settings.edit(), baseUrlFail = true)
        }
    }

    fun login(): Call<SuccessResponse> =
        service.loginToSelfoss(config.userLogin, config.userPassword)

    fun readItems(tag: String?, sourceId: Long?, search: String?): Call<List<Item>> =
        getItems("read", tag, sourceId, search)

    fun newItems(tag: String?, sourceId: Long?, search: String?): Call<List<Item>> =
        getItems("unread", tag, sourceId, search)

    fun starredItems(tag: String?, sourceId: Long?, search: String?): Call<List<Item>> =
        getItems("starred", tag, sourceId, search)

    private fun getItems(type: String, tag: String?, sourceId: Long?, search: String?): Call<List<Item>> =
        service.getItems(type, tag, sourceId, search, userName, password)

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

// codebeat:enable[ARITY,TOO_MANY_FUNCTIONS]