package app.totv.retro.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/** Accepts `true`/`false`, `0`/`1` and `"0"`/`"1"`. */
object FlexBoolSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexBool", PrimitiveKind.BOOLEAN)
    override fun deserialize(decoder: Decoder): Boolean {
        val el = (decoder as JsonDecoder).decodeJsonElement()
        if (el is JsonNull || el !is JsonPrimitive) return false
        return el.booleanOrNull ?: (el.longOrNull?.let { it != 0L } ?: (el.content.lowercase() in setOf("true", "yes")))
    }
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}

/** Accepts numbers and numeric strings; anything else becomes 0. */
object FlexLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexLong", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): Long {
        val el = (decoder as JsonDecoder).decodeJsonElement()
        if (el is JsonNull || el !is JsonPrimitive) return 0L
        return el.longOrNull ?: el.content.toDoubleOrNull()?.toLong() ?: 0L
    }
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

@Serializable
data class PlatformCores(val android: List<String> = emptyList(), val web: List<String> = emptyList())

@Serializable
data class BiosFile(
    val file: String,
    val required: Boolean = false,
    val md5: String? = null,
    val present: Boolean = false,
)

@Serializable
data class Platform(
    val id: String,
    val name: String = "",
    val extensions: List<String> = emptyList(),
    val cores: PlatformCores = PlatformCores(),
    val bios: List<BiosFile> = emptyList(),
)

@Serializable
data class MediaVersion(
    val id: Long,
    val label: String = "",
    val size: Long? = null,
)

@Serializable
data class GameItem(
    val id: Long,
    val title: String = "",
    val platform: String? = null,
    val media: List<MediaVersion> = emptyList(),
    val resumeStateId: Long? = null,
)

@Serializable
data class SramInfo(val core: String = "", val size: Long = 0, val updatedAt: Long = 0)

@Serializable
data class SaveState(
    val id: Long,
    @Serializable(with = FlexLongSerializer::class) val slot: Long = 0,
    @Serializable(with = FlexBoolSerializer::class) val auto: Boolean = false,
    val core: String = "",
    val mediaId: Long? = null,
    val size: Long = 0,
    @Serializable(with = FlexLongSerializer::class) val createdAt: Long = 0,
    val thumbUrl: String? = null,
    val label: String? = null,
)

@Serializable
data class Saves(val sram: SramInfo? = null, val states: List<SaveState> = emptyList())

@Serializable
data class SessionReply(val ok: Boolean = true, val stop: Boolean = false, val plays: Long? = null, val totalMs: Long? = null)

@Serializable
internal data class SessionBody(val state: String, val mediaId: Long?, val core: String, val playedMs: Long)

@Serializable
internal data class IdReply(val id: Long)
