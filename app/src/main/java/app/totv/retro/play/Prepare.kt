package app.totv.retro.play

import app.totv.retro.api.ApiException
import app.totv.retro.api.GameItem
import app.totv.retro.api.Platform
import app.totv.retro.api.SaveState
import app.totv.retro.api.Saves
import app.totv.retro.api.TotvApi
import app.totv.retro.cache.RomCache
import app.totv.retro.core.CoreChoice
import app.totv.retro.core.CoreSelector
import java.io.File
import java.security.MessageDigest

/** Something the user should be told, mapped to a message by the UI. */
class PlayException(val kind: Kind, val detail: String = "", cause: Throwable? = null) : Exception("$kind $detail", cause) {
    enum class Kind { BAD_REQUEST, NETWORK, AUTH, NOT_FOUND, SCHEDULE, NO_CORE, BIOS_MISSING, NO_MEDIA }
}

/** Everything needed to start the emulator. */
class Prepared(
    val item: GameItem,
    val platform: Platform,
    val core: String,
    val rom: File,
    val mediaId: Long,
    val sram: ByteArray?,
    val resumeState: ByteArray?,
    val resumeStateId: Long?,
    val resumeSkipped: Boolean,
    val saves: Saves,
)

/** Network + disk preparation of a game launch (no Android UI): platforms, item, BIOS, ROM, SRAM, state. */
class Preparer(
    private val api: TotvApi,
    private val romCache: RomCache,
    private val systemDir: File,
    private val embedded: Set<String>,
    private val abi: String,
    private val status: (Step, Long, Long) -> Unit = { _, _, _ -> },
) {
    enum class Step { SERVER, BIOS, ROM, SAVE }

    fun prepare(req: PlayRequest): Prepared {
        try {
            return doPrepare(req)
        } catch (e: ApiException) {
            throw when {
                e.isAuth -> PlayException(PlayException.Kind.AUTH, e.message.orEmpty(), e)
                e.isForbidden -> PlayException(PlayException.Kind.SCHEDULE, e.message.orEmpty(), e)
                e.isNotFound -> PlayException(PlayException.Kind.NOT_FOUND, e.message.orEmpty(), e)
                else -> PlayException(PlayException.Kind.NETWORK, e.message.orEmpty(), e)
            }
        }
    }

    private fun doPrepare(req: PlayRequest): Prepared {
        status(Step.SERVER, 0, 0)
        val item = api.item(req.itemId)
        val platforms = api.platforms()
        val platformId = req.platform ?: item.platform ?: throw PlayException(PlayException.Kind.BAD_REQUEST, "platform")
        val platform = platforms.firstOrNull { it.id == platformId } ?: Platform(id = platformId)
        // Saves are optional for starting: a failure here (other than auth) just means "no states listed".
        val saves = try { api.saves(req.itemId) } catch (e: ApiException) { if (e.isAuth || e.isForbidden) throw e else Saves() }

        val wantedState: SaveState? = req.stateId?.let { id -> saves.states.firstOrNull { it.id == id } }
        val choice = CoreSelector.select(platform, embedded, abi, wantedState?.core)
        val core = (choice as? CoreChoice.Selected)?.core ?: throw PlayException(PlayException.Kind.NO_CORE, platformId)
        val resumeOk = wantedState != null && CoreSelector.canLoadState(core, wantedState.core)

        // BIOS
        for (b in platform.bios) {
            if (!b.present) {
                if (b.required) throw PlayException(PlayException.Kind.BIOS_MISSING, b.file)
                continue
            }
            val dest = File(systemDir, safeName(b.file))
            if (dest.isFile && dest.length() > 0 && (b.md5.isNullOrBlank() || md5(dest).equals(b.md5, true))) continue
            status(Step.BIOS, 0, 0)
            api.downloadBios(platformId, b.file, dest) { d, t -> status(Step.BIOS, d, t) }
        }

        // ROM
        val mediaId = req.mediaId ?: item.media.firstOrNull()?.id ?: throw PlayException(PlayException.Kind.NO_MEDIA)
        val rom = romCache.get(mediaId) ?: run {
            status(Step.ROM, 0, 0)
            val tmp = romCache.target(mediaId, "download.tmp")
            val name = api.downloadRom(mediaId, extract = true, dest = tmp) { d, t -> status(Step.ROM, d, t) }
            val finalFile = File(tmp.parentFile, romFileName(name, platform))
            if (!tmp.renameTo(finalFile)) throw PlayException(PlayException.Kind.NETWORK, "rename")
            finalFile
        }
        romCache.trim(keepMediaId = mediaId)

        // Saves
        status(Step.SAVE, 0, 0)
        val sram = api.sram(req.itemId, core)
        val state = if (resumeOk) api.state(wantedState!!.id) else null
        return Prepared(item, platform, core, rom, mediaId, sram, state, wantedState?.id, req.stateId != null && !resumeOk, saves)
    }

    companion object {
        fun safeName(n: String) = n.substringAfterLast('/').substringAfterLast('\\').ifBlank { "file.bin" }

        /** The cores detect the content type by extension: keep the server's name, unless it is a zip. */
        fun romFileName(serverName: String?, platform: Platform): String {
            val n = if (serverName == null) "rom" else safeName(serverName)
            if (serverName != null && !n.endsWith(".zip", ignoreCase = true) && n.contains('.')) return n
            val ext = platform.extensions.firstOrNull { !it.equals(".zip", true) } ?: defaultExt(platform.id)
            val stem = n.substringBeforeLast('.', n).ifBlank { "rom" }
            return stem + (if (ext.startsWith(".")) ext else ".$ext")
        }

        fun defaultExt(platform: String) = when (platform) {
            "nes" -> ".nes"; "snes" -> ".sfc"; "gb" -> ".gb"; "gbc" -> ".gbc"; "gba" -> ".gba"
            "md" -> ".md"; "sms" -> ".sms"; "gg" -> ".gg"; "pce" -> ".pce"; else -> ".bin"
        }

        fun md5(f: File): String {
            val md = MessageDigest.getInstance("MD5")
            f.inputStream().use { i -> val buf = ByteArray(64 * 1024); while (true) { val n = i.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
