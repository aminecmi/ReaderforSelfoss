package apps.amine.bou.readerforselfoss.api.mercury

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MercuryApi(private val key: String, shouldLog: Boolean) {
    private val service: MercuryService

    init {

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = if (shouldLog) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val gson = GsonBuilder()
                .setLenient()
                .create()
        val retrofit =
                Retrofit
                        .Builder()
                        .baseUrl("https://mercury.postlight.com")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build()
        service = retrofit.create(MercuryService::class.java)
    }

    fun parseUrl(url: String): Call<ParsedContent> {
        return service.parseUrl(url, this.key)
    }
}
