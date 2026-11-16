package app.totv.retro

import android.view.KeyEvent
import app.totv.retro.api.Platform
import app.totv.retro.cache.RomCache
import app.totv.retro.input.PadMapper
import app.totv.retro.play.Preparer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MiscTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun put(c: RomCache, id: Long, size: Int, age: Long) {
        val f = c.target(id, "g$id.gb"); f.writeBytes(ByteArray(size)); f.setLastModified(age); f.parentFile!!.setLastModified(age)
    }

    @Test fun romCacheEvictsLeastRecentlyUsed() {
        val c = RomCache(tmp.newFolder("roms"), limitBytes = 1000)
        put(c, 1, 400, 1_000); put(c, 2, 400, 2_000); put(c, 3, 400, 3_000)
        c.trim(keepMediaId = null)
        assertNull(c.get(1)); assertNotNull(c.get(2)); assertNotNull(c.get(3))
        assertTrue(c.totalSize() <= 1000)
    }

    @Test fun romCacheGetRefreshesRecency() {
        val c = RomCache(tmp.newFolder("roms"), limitBytes = 1000)
        put(c, 1, 400, 1_000); put(c, 2, 400, 2_000)
        assertNotNull(c.get(1))             // touch the oldest: it becomes the newest
        put(c, 3, 400, System.currentTimeMillis() - 10)
        c.trim()
        assertNull(c.get(2)); assertNotNull(c.get(1)); assertNotNull(c.get(3))
    }

    @Test fun romCacheNeverEvictsKeptEntry() {
        val c = RomCache(tmp.newFolder("roms"), limitBytes = 100)
        put(c, 1, 400, 1_000)
        c.trim(keepMediaId = 1)
        assertNotNull(c.get(1))
    }

    @Test fun romCacheIgnoresPartialAndSanitisesNames() {
        val c = RomCache(tmp.newFolder("roms"))
        val t = c.target(5, "a/b:c.nes")
        assertEquals("a_b_c.nes", t.name)
        File(t.parentFile, "x.nes.part").writeBytes(ByteArray(10))
        assertNull(c.get(5))
    }

    @Test fun romFileNameKeepsServerNameOrUsesPlatformExtension() {
        val gba = Platform("gba", extensions = listOf(".zip", ".gba"))
        assertEquals("Game (USA).gba", Preparer.romFileName("Game (USA).gba", gba))
        assertEquals("Game.gba", Preparer.romFileName("Game.zip", gba))
        assertEquals("rom.gba", Preparer.romFileName(null, gba))
        assertEquals("x.nes", Preparer.romFileName("../x.nes", Platform("nes")))
    }

    @Test fun padMapperSwapsAbxyAndMapsRemote() {
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, PadMapper.gameKey(KeyEvent.KEYCODE_BUTTON_A, true))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, PadMapper.gameKey(KeyEvent.KEYCODE_BUTTON_B, true))
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, PadMapper.gameKey(KeyEvent.KEYCODE_BUTTON_X, true))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, PadMapper.gameKey(KeyEvent.KEYCODE_BUTTON_L1, true))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, PadMapper.gameKey(KeyEvent.KEYCODE_DPAD_CENTER, false))
        assertNull(PadMapper.gameKey(KeyEvent.KEYCODE_DPAD_CENTER, true))
        assertEquals(KeyEvent.KEYCODE_BUTTON_START, PadMapper.gameKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, PadMapper.gameKey(KeyEvent.KEYCODE_DPAD_LEFT, false))
        assertNull(PadMapper.gameKey(KeyEvent.KEYCODE_BACK, false))
        assertTrue(PadMapper.isMenuKey(KeyEvent.KEYCODE_BACK))
        assertFalse(PadMapper.isMenuKey(KeyEvent.KEYCODE_BUTTON_START))
    }

    @Test fun analogToDpadWithDeadzone() {
        assertEquals(0f, PadMapper.dpadAxis(0.3f, 0f), 0f)
        assertEquals(1f, PadMapper.dpadAxis(0.8f, 0f), 0f)
        assertEquals(-1f, PadMapper.dpadAxis(-0.8f, 0f), 0f)
        assertEquals(1f, PadMapper.dpadAxis(0f, 1f), 0f)
        assertEquals(-1f, PadMapper.dpadAxis(0.9f, -1f), 0f)
    }
}
