package apps.amine.bou.readerforselfoss.api.mercury

import apps.amine.bou.readerforselfoss.interceptors.ApiLoggingInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MercuryApi(shouldLog: Boolean) {
    private val service: MercuryService

    init {

        val interceptor = ApiLoggingInterceptor()
        interceptor.level = if (shouldLog) {
            ApiLoggingInterceptor.Level.BODY
        } else {
            ApiLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl("https://www.amine-bou.fr")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        service = retrofit.create(MercuryService::class.java)
    }

    fun parseUrl(url: String): Call<ParsedContent> {
        return service.parseUrl(url)
    }
}
