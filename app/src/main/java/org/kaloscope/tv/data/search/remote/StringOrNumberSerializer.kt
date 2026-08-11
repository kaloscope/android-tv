package org.kaloscope.tv.data.search.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

internal object StringOrNumberSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive
            ?: throw SerializationException("Expected a string or number")
        // Dynamic workflow output may represent scalar metadata as either JSON type.
        if (primitive === JsonNull || (!primitive.isString && primitive.booleanOrNull != null)) {
            throw SerializationException("Expected a string or number")
        }
        return primitive.content
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
