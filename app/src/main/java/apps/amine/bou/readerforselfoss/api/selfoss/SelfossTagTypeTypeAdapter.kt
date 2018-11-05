package apps.amine.bou.readerforselfoss.api.selfoss

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

internal class SelfossTagTypeTypeAdapter : JsonDeserializer<SelfossTagType> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SelfossTagType? =
        if (json.isJsonArray) {
            SelfossTagType(json.asJsonArray.joinToString(",") { it.toString() })
        } else {
            SelfossTagType(json.toString())
        }
}
