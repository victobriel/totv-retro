package app.totv.retro.api

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.URLDecoder

/** HTTP error from the Totv games API. [code] is the HTTP status, or 0 for transport failures. */
class ApiException(val code: Int, message: String, cause: Throwable? = null) : IOException(message, cause) {
    val isAuth get() = code == 401
    val isForbidden get() = code == 403
    val isNotFound get() = code == 404
}

/**
 * Client for the Totv "games API" (docs/games-api.md of the Totv server). Blocking: call it from a background
 * dispatcher. It only ever sends the scoped *player token* (`X-Totv-Token`).
 */
class TotvApi(server: String, private val token: String, private val http: OkHttpClient) {
    private val base: HttpUrl = normalize(server)

    private fun url(vararg segments: String): HttpUrl.Builder {
        val b = base.newBuilder()
        // keep a base path if the server runs under one
        segments.forEach { seg -> seg.split('/').filter { it.isNotEmpty() }.forEach { b.addPathSegment(it) } }
        return b
    }

    private fun req(url: HttpUrl): Request.Builder = Request.Builder().url(url).header("X-Totv-Token", token)

    private fun <T> call(r: Request, block: (Response) -> T): T {
        val resp = try {
            http.newCall(r).execute()
        } catch (e: IOException) {
            throw ApiException(0, e.message ?: "network error", e)
        }
        resp.use {
            if (!it.isSuccessful) throw ApiException(it.code, "HTTP ${it.code} ${r.method} ${r.url.encodedPath}")
            return block(it)
        }
    }

    private inline fun <reified T> getJson(url: HttpUrl): T =
        call(req(url).get().build()) { json.decodeFromString<T>(it.body!!.string()) }

    fun platforms(): List<Platform> = getJson(url("api", "games", "platforms").build())

    fun item(itemId: Long): GameItem = getJson(url("api", "items", itemId.toString()).build())

    fun saves(itemId: Long): Saves = getJson(url("api", "games", itemId.toString(), "saves").build())

    /** Stored SRAM, or null when the server has none (404). */
    fun sram(itemId: Long, core: String): ByteArray? {
        val u = url("api", "games", itemId.toString(), "sram").addQueryParameter("core", core).build()
        return try {
            call(req(u).get().build()) { it.body!!.bytes() }
        } catch (e: ApiException) {
            if (e.isNotFound) null else throw e
        }
    }

    fun putSram(itemId: Long, core: String, mediaId: Long?, data: ByteArray) {
        val u = url("api", "games", itemId.toString(), "sram").addQueryParameter("core", core)
            .apply { if (mediaId != null) addQueryParameter("mediaId", mediaId.toString()) }.build()
        call(req(u).put(data.toRequestBody(OCTET)).build()) { }
    }

    /** Uploads a state; returns the new state id. [slot] 1..9 for manual states, ignored when [auto]. */
    fun postState(itemId: Long, core: String, mediaId: Long?, slot: Int, auto: Boolean, label: String?, data: ByteArray): Long {
        val u = url("api", "games", itemId.toString(), "states")
            .addQueryParameter("core", core)
            .apply { if (mediaId != null) addQueryParameter("mediaId", mediaId.toString()) }
            .addQueryParameter("slot", if (auto) "0" else slot.toString())
            .addQueryParameter("auto", if (auto) "1" else "0")
            .apply { if (!label.isNullOrBlank()) addQueryParameter("label", label.take(60)) }
            .build()
        return call(req(u).post(data.toRequestBody(OCTET)).build()) { json.decodeFromString<IdReply>(it.body!!.string()).id }
    }

    fun putThumb(stateId: Long, png: ByteArray, mime: String = "image/png") {
        val u = url("api", "games", "states", stateId.toString(), "thumb").build()
        call(req(u).put(png.toRequestBody(mime.toMediaType())).build()) { }
    }

    fun state(stateId: Long): ByteArray =
        call(req(url("api", "games", "states", stateId.toString()).build()).get().build()) { it.body!!.bytes() }

    /** Thumbnail bytes from a (server-relative or absolute) [thumbUrl] as listed in `saves`. */
    fun thumb(thumbUrl: String): ByteArray? = try {
        call(req(resolve(thumbUrl)).get().build()) { it.body!!.bytes() }
    } catch (e: ApiException) {
        if (e.isNotFound) null else throw e
    }

    /** Sends a session event. `stop:true` in the reply means "outside the access schedule: close the game". */
    fun session(itemId: Long, state: String, mediaId: Long?, core: String, playedMs: Long): SessionReply {
        val body = json.encodeToString(SessionBody.serializer(), SessionBody(state, mediaId, core, playedMs))
        val u = url("api", "games", itemId.toString(), "session").build()
        return call(req(u).post(body.toRequestBody(JSON)).build()) {
            runCatching { json.decodeFromString<SessionReply>(it.body!!.string()) }.getOrDefault(SessionReply())
        }
    }

    /** Downloads the ROM into [dest] (via a .part file). Returns the file name announced by the server, if any. */
    fun downloadRom(mediaId: Long, extract: Boolean, dest: File, progress: (Long, Long) -> Unit = { _, _ -> }): String? {
        val u = url("api", "games", "rom", mediaId.toString()).apply { if (extract) addQueryParameter("extract", "1") }.build()
        return call(req(u).get().build()) { resp ->
            copyTo(resp, dest, progress)
            romName(resp)
        }
    }

    fun downloadBios(platform: String, file: String, dest: File, progress: (Long, Long) -> Unit = { _, _ -> }) {
        val u = url("api", "games", "bios", platform, file).build()
        call(req(u).get().build()) { copyTo(it, dest, progress) }
    }

    /** Resolves a server-relative path (`/api/...`, possibly already prefixed by the server base path). */
    fun resolve(path: String): HttpUrl {
        if (path.startsWith("http://") || path.startsWith("https://")) return path.toHttpUrl()
        val basePath = base.encodedPath.trimEnd('/')
        val rel = if (path.startsWith("/")) path else "/$path"
        val full = if (basePath.isNotEmpty() && !(rel == basePath || rel.startsWith("$basePath/"))) basePath + rel else rel
        return base.newBuilder().encodedPath("/").build().resolve(full)
            ?: throw ApiException(0, "bad path: $path")
    }

    private fun copyTo(resp: Response, dest: File, progress: (Long, Long) -> Unit) {
        val part = File(dest.parentFile, dest.name + ".part")
        dest.parentFile?.mkdirs()
        val total = resp.body!!.contentLength()
        try {
            resp.body!!.byteStream().use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var last = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (done - last >= 256 * 1024) { progress(done, total); last = done }
                    }
                    progress(done, total)
                }
            }
            if (dest.exists()) dest.delete()
            if (!part.renameTo(dest)) throw IOException("cannot move ${part.name}")
        } finally {
            part.delete()
        }
    }

    companion object {
        private val OCTET = "application/octet-stream".toMediaType()
        private val JSON = "application/json".toMediaType()
        internal val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false; encodeDefaults = true }

        /** Accepts `host:port`, with or without scheme; defaults to http (LAN servers). */
        fun normalize(server: String): HttpUrl {
            val s = server.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
            val u = s.toHttpUrl()
            return u.newBuilder().query(null).fragment(null).build()
        }

        /** `X-Totv-Rom-Name`, percent-decoded when `X-Totv-Rom-Name-Encoding: uri`. */
        fun romName(resp: Response): String? {
            val raw = resp.header("X-Totv-Rom-Name") ?: return null
            val name = if (resp.header("X-Totv-Rom-Name-Encoding") == "uri") runCatching { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrDefault(raw) else raw
            return name.substringAfterLast('/').substringAfterLast('\\').ifBlank { null }
        }
    }
}
