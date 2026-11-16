package app.totv.retro

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/** A tiny in-process fake of the Totv server: an OkHttp interceptor answers every call (no sockets). */
class FakeServer(private val handler: (Recorded) -> Reply) {
    class Recorded(val method: String, val url: okhttp3.HttpUrl, val headers: okhttp3.Headers, val body: ByteArray, val contentType: String? = null) {
        val path: String get() = url.encodedPath
    }
    class Reply(val code: Int = 200, val body: ByteArray = ByteArray(0), val type: String = "application/json", val headers: Map<String, String> = emptyMap()) {
        constructor(code: Int, json: String) : this(code, json.toByteArray())
    }

    val calls = mutableListOf<Recorded>()

    val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        val req: Request = chain.request()
        val buf = Buffer()
        req.body?.writeTo(buf)
        val rec = Recorded(req.method, req.url, req.headers, buf.readByteArray(), req.body?.contentType()?.toString())
        calls += rec
        val r = handler(rec)
        Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(r.code).message("fake")
            .apply { r.headers.forEach { (k, v) -> header(k, v) } }
            .body(r.body.toResponseBody(r.type.toMediaType())).build()
    }).build()
}
