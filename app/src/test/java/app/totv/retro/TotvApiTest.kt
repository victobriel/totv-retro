package app.totv.retro

import app.totv.retro.api.ApiException
import app.totv.retro.api.TotvApi
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TotvApiTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun api(server: FakeServer, base: String = "http://tv.local:3000") = TotvApi(base, "tok123", server.client)

    @Test fun platformsParsedAndTokenSent() {
        val s = FakeServer {
            FakeServer.Reply(200, """[{"id":"gba","name":"GBA","extensions":[".gba",".zip"],"cores":{"android":["mgba","gpsp"],"web":["mgba"]},
              "bios":[{"file":"gba_bios.bin","required":false,"md5":"abc","present":true}],"extra":1}]""")
        }
        val p = api(s).platforms().single()
        assertEquals("gba", p.id)
        assertEquals(listOf("mgba", "gpsp"), p.cores.android)
        assertTrue(p.bios.single().present)
        assertEquals("/api/games/platforms", s.calls.single().path)
        assertEquals("tok123", s.calls.single().headers["X-Totv-Token"])
    }

    @Test fun itemWithMediaAndNullResume() {
        val s = FakeServer { FakeServer.Reply(200, """{"id":7,"title":"Zelda","platform":"gbc","media":[{"id":70,"label":"USA","size":1024}],"resumeStateId":null}""") }
        val it = api(s).item(7)
        assertEquals(70L, it.media.single().id)
        assertNull(it.resumeStateId)
    }

    @Test fun savesAcceptNumericOrBooleanAuto() {
        val s = FakeServer {
            FakeServer.Reply(200, """{"sram":{"core":"fceumm","size":8192,"updatedAt":1},"states":[
              {"id":1,"slot":0,"auto":1,"core":"fceumm","mediaId":5,"size":10,"createdAt":1700000000000,"thumbUrl":"/api/games/states/1/thumb","label":"Auto"},
              {"id":2,"slot":"3","auto":false,"core":"fceumm","createdAt":"1700000000001","thumbUrl":null}]}""")
        }
        val saves = api(s).saves(9)
        assertTrue(saves.states[0].auto)
        assertFalse(saves.states[1].auto)
        assertEquals(3L, saves.states[1].slot)
        assertEquals(1700000000001L, saves.states[1].createdAt)
        assertEquals("fceumm", saves.sram?.core)
    }

    @Test fun sram404IsNull_otherErrorsThrow() {
        val s404 = FakeServer { FakeServer.Reply(404, "{}") }
        assertNull(api(s404).sram(1, "fceumm"))
        assertEquals("/api/games/1/sram", s404.calls.single().path)
        assertEquals("fceumm", s404.calls.single().url.queryParameter("core"))
        val s500 = FakeServer { FakeServer.Reply(500, "{}") }
        try { api(s500).sram(1, "x"); fail() } catch (e: ApiException) { assertEquals(500, e.code) }
    }

    @Test fun putSramSendsBinaryBodyAndMedia() {
        val s = FakeServer { FakeServer.Reply(200, """{"ok":true}""") }
        api(s).putSram(4, "gambatte", 44, byteArrayOf(1, 2, 3))
        val c = s.calls.single()
        assertEquals("PUT", c.method)
        assertEquals("/api/games/4/sram", c.path)
        assertEquals("44", c.url.queryParameter("mediaId"))
        assertArrayEquals(byteArrayOf(1, 2, 3), c.body)
    }

    @Test fun postStateAutoAndManualQuery() {
        val s = FakeServer { FakeServer.Reply(200, """{"id":99}""") }
        val a = api(s)
        assertEquals(99L, a.postState(5, "fceumm", 50, 0, auto = true, label = null, data = byteArrayOf(9)))
        val auto = s.calls[0].url
        assertEquals("1", auto.queryParameter("auto"))
        assertEquals("POST", s.calls[0].method)
        a.postState(5, "fceumm", 50, 4, auto = false, label = "Slot 4", data = byteArrayOf(9))
        val manual = s.calls[1].url
        assertEquals("0", manual.queryParameter("auto"))
        assertEquals("4", manual.queryParameter("slot"))
        assertEquals("Slot 4", manual.queryParameter("label"))
    }

    @Test fun thumbPutUsesMime() {
        val s = FakeServer { FakeServer.Reply(200, "{}") }
        api(s).putThumb(12, byteArrayOf(1))
        assertEquals("/api/games/states/12/thumb", s.calls.single().path)
        assertEquals("image/png", s.calls.single().contentType?.substringBefore(';'))
    }

    @Test fun sessionBodyAndStopReply() {
        val s = FakeServer { FakeServer.Reply(200, """{"ok":false,"stop":true}""") }
        val r = api(s).session(3, "heartbeat", 30, "mgba", 61000)
        assertTrue(r.stop)
        val body = String(s.calls.single().body)
        assertTrue(body, body.contains("\"state\":\"heartbeat\""))
        assertTrue(body, body.contains("\"playedMs\":61000"))
        assertTrue(body, body.contains("\"core\":\"mgba\""))
    }

    @Test fun authAndForbiddenFlags() {
        val e401 = try { api(FakeServer { FakeServer.Reply(401, "{}") }).item(1); null } catch (e: ApiException) { e }
        assertTrue(e401!!.isAuth)
        val e403 = try { api(FakeServer { FakeServer.Reply(403, "{}") }).item(1); null } catch (e: ApiException) { e }
        assertTrue(e403!!.isForbidden)
    }

    @Test fun networkFailureIsCodeZero() {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { throw java.io.IOException("boom") }.build()
        try { TotvApi("tv.local:3000", "t", client).platforms(); fail() } catch (e: ApiException) { assertEquals(0, e.code) }
    }

    @Test fun romDownloadExtractQueryAndNameDecoding() {
        val data = ByteArray(300_000) { (it % 251).toByte() }
        val s = FakeServer {
            FakeServer.Reply(200, data, "application/octet-stream", mapOf("X-Totv-Rom-Name" to "Pok%C3%A9mon%20Gold.gbc", "X-Totv-Rom-Name-Encoding" to "uri"))
        }
        val dest = tmp.newFolder("c").resolve("rom.tmp")
        var lastDone = 0L
        val name = api(s).downloadRom(77, extract = true, dest = dest) { d, _ -> lastDone = d }
        assertEquals("Pokémon Gold.gbc", name)
        assertEquals("1", s.calls.single().url.queryParameter("extract"))
        assertEquals("/api/games/rom/77", s.calls.single().path)
        assertArrayEquals(data, dest.readBytes())
        assertEquals(data.size.toLong(), lastDone)
        assertFalse(dest.resolveSibling("rom.tmp.part").exists())
    }

    @Test fun biosPath() {
        val s = FakeServer { FakeServer.Reply(200, byteArrayOf(1, 2), "application/octet-stream") }
        val dest = tmp.newFile("bios.bin")
        api(s).downloadBios("gba", "gba_bios.bin", dest)
        assertEquals("/api/games/bios/gba/gba_bios.bin", s.calls.single().path)
        assertArrayEquals(byteArrayOf(1, 2), dest.readBytes())
    }

    @Test fun serverUnderBasePath() {
        val s = FakeServer { FakeServer.Reply(200, "[]") }
        api(s, "https://example.com/totv/").platforms()
        assertEquals("/totv/api/games/platforms", s.calls.single().path)
    }

    @Test fun thumbUrlResolution() {
        val a = api(FakeServer { FakeServer.Reply(200, "") })
        assertEquals("http://tv.local:3000/api/games/states/1/thumb", a.resolve("/api/games/states/1/thumb").toString())
        val under = api(FakeServer { FakeServer.Reply(200, "") }, "http://h/totv")
        // already prefixed by the base path: not doubled
        assertEquals("http://h/totv/api/games/states/1/thumb", under.resolve("/totv/api/games/states/1/thumb").toString())
        // not prefixed: the base path is added
        assertEquals("http://h/totv/api/games/states/1/thumb", under.resolve("/api/games/states/1/thumb").toString())
        assertEquals("http://x.org/t", a.resolve("http://x.org/t").toString())
    }

    @Test fun serverWithoutSchemeDefaultsToHttp() {
        assertEquals("http://192.168.1.5:3000/", TotvApi.normalize("192.168.1.5:3000").toString())
    }
}
