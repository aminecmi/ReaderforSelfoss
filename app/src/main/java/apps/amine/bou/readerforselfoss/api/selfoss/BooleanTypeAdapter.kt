package apps.amine.bou.readerforselfoss.api.selfoss

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type


internal class BooleanTypeAdapter : JsonDeserializer<Boolean> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Boolean? =
        try {
            json.asInt == 1
        } catch (e: Exception) {
            json.asBoolean
        }
}
