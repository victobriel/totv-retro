package app.totv.retro

import app.totv.retro.cache.RomCache
import app.totv.retro.core.CoreChoice
import app.totv.retro.core.CoreSelector
import app.totv.retro.play.RewindBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RewindTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun ringDropsOldestAndPopsNewestFirst() {
        val b = RewindBuffer(capBytes = 100, minFrames = 2)
        for (i in 1..5) assertTrue(b.push(ByteArray(40) { i.toByte() }))
        assertEquals(2, b.size)
        assertEquals(5, b.pop()!![0].toInt())
    }

    @Test fun rejectsStatesTooLargeAndEmptyBufferPopsNull() {
        val b = RewindBuffer(capBytes = 32 * 1024 * 1024)
        assertFalse(b.push(ByteArray(4 * 1024 * 1024)))   // PS1-sized
        assertTrue(b.push(ByteArray(300 * 1024)))          // GBA/SNES-sized
        b.clear(); assertNull(b.pop()); assertTrue(b.isEmpty)
    }

    @Test fun defaults() {
        assertTrue(RewindBuffer.defaultOn("snes")); assertFalse(RewindBuffer.defaultOn("psx"))
    }

    @Test fun psxSelectsPcsx() {
        val c = CoreSelector.select("psx", emptyList(), setOf("pcsx_rearmed"), "armeabi-v7a")
        assertEquals("pcsx_rearmed", (c as CoreChoice.Selected).core)
    }

    @Test fun trimKeepsAllDiscsAndPlaylistDir() {
        val c = RomCache(tmp.newFolder("roms"), limitBytes = 10)
        for (id in 1L..3L) c.target(id, "d$id.cue").writeBytes(ByteArray(100))
        val m3u = c.playlistFile(1); m3u.writeText("x")
        c.trim(setOf(1L, 2L))
        assertNotNull(c.get(1)); assertNotNull(c.get(2)); assertNull(c.get(3)); assertTrue(m3u.isFile)
        assertEquals("../2/d2.cue", c.relativeToPlaylistDir(c.get(2)!!))
    }
}
