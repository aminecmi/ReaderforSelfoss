package apps.amine.bou.readerforselfoss.api.mercury

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MercuryService {
    @GET("parser")
    fun parseUrl(@Query("url") url: String, @Header("x-api-key") key: String): Call<ParsedContent>
}
