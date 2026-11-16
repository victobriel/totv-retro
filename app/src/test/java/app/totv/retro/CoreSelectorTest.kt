package app.totv.retro

import app.totv.retro.core.CoreChoice
import app.totv.retro.core.CoreSelector
import app.totv.retro.core.Reason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreSelectorTest {
    private val base32 = setOf("fceumm", "gambatte", "gpsp", "smsplus", "mednafen_pce_fast")
    private val base64 = setOf("fceumm", "gambatte", "mgba", "smsplus", "mednafen_pce_fast")

    private fun sel(p: String, server: List<String>, emb: Set<String>, abi: String, st: String? = null) =
        CoreSelector.select(p, server, emb, abi, st)

    @Test fun serverPreferenceFirstEmbedded() {
        val c = sel("gb", listOf("sameboy", "gambatte"), base64, "arm64-v8a") as CoreChoice.Selected
        assertEquals("gambatte", c.core)
        assertEquals(Reason.SERVER_PREFERENCE, c.reason)
    }

    @Test fun gbaPicksGpspOnArmv7AndMgbaOnArm64EvenIfServerSaysOtherwise() {
        assertEquals("gpsp", (sel("gba", listOf("mgba", "gpsp"), base32 + "mgba", "armeabi-v7a") as CoreChoice.Selected).core)
        assertEquals("mgba", (sel("gba", listOf("gpsp", "mgba"), base64 + "gpsp", "arm64-v8a") as CoreChoice.Selected).core)
    }

    @Test fun gbaWithOnlyOneEmbeddedUsesIt() {
        assertEquals("gpsp", (sel("gba", listOf("mgba"), base32, "armeabi-v7a") as CoreChoice.Selected).core)
        assertEquals("mgba", (sel("gba", listOf("gpsp"), base64, "arm64-v8a") as CoreChoice.Selected).core)
    }

    @Test fun builtinFallbackWhenServerListEmptyOrUnknown() {
        val c = sel("nes", emptyList(), base64, "arm64-v8a") as CoreChoice.Selected
        assertEquals("fceumm", c.core); assertEquals(Reason.BUILTIN_PREFERENCE, c.reason)
        assertEquals("fceumm", (sel("nes", listOf("nestopia"), base64, "arm64-v8a") as CoreChoice.Selected).core)
    }

    @Test fun noCoreForSnesAndMdUnlessEmbedded() {
        assertTrue(sel("snes", listOf("mednafen_supafaust"), base64, "arm64-v8a") is CoreChoice.None)
        assertTrue(sel("md", emptyList(), base64, "arm64-v8a") is CoreChoice.None)
        assertEquals("mednafen_supafaust", (sel("snes", listOf("mednafen_supafaust"), base64 + "mednafen_supafaust", "arm64-v8a") as CoreChoice.Selected).core)
    }

    @Test fun mdPrefersBlastemThenClownmdemu() {
        val emb = base64 + "blastem" + "clownmdemu"
        assertEquals("blastem", (sel("md", emptyList(), emb, "arm64-v8a") as CoreChoice.Selected).core)
        assertEquals("clownmdemu", (sel("md", emptyList(), base64 + "clownmdemu", "arm64-v8a") as CoreChoice.Selected).core)
        assertEquals("clownmdemu", (sel("md", listOf("clownmdemu", "blastem"), emb, "arm64-v8a") as CoreChoice.Selected).core)
    }

    @Test fun stateCoreWinsWhenEmbeddedAndSupportsPlatform() {
        val emb = base64 + "gpsp"
        val c = sel("gba", listOf("mgba"), emb, "arm64-v8a", st = "gpsp") as CoreChoice.Selected
        assertEquals("gpsp", c.core); assertEquals(Reason.STATE_CORE, c.reason)
    }

    @Test fun stateCoreIgnoredWhenNotEmbeddedOrWrongPlatform() {
        assertEquals("mgba", (sel("gba", emptyList(), base64, "arm64-v8a", st = "gpsp") as CoreChoice.Selected).core)
        assertEquals("fceumm", (sel("nes", emptyList(), base64, "arm64-v8a", st = "gambatte") as CoreChoice.Selected).core)
    }

    @Test fun smsAndGgShareSmsplus() {
        assertEquals("smsplus", (sel("sms", emptyList(), base32, "armeabi-v7a") as CoreChoice.Selected).core)
        assertEquals("smsplus", (sel("gg", emptyList(), base32, "armeabi-v7a") as CoreChoice.Selected).core)
    }

    @Test fun unknownPlatformHasNoCore() {
        assertTrue(sel("saturn", listOf("x"), base64, "arm64-v8a") is CoreChoice.None)
    }

    @Test fun statesAreCoreSpecific() {
        assertTrue(CoreSelector.canLoadState("mgba", "mgba"))
        assertTrue(!CoreSelector.canLoadState("mgba", "gpsp"))
    }
}
