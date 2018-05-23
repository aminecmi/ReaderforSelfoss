package apps.amine.bou.readerforselfoss.api.mercury

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MercuryService {
    @GET("parser.php")
    fun parseUrl(@Query("link") link: String): Call<ParsedContent>
}
