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
    /** Disc labels of a multi-disc set (empty when single disc); [rom] is then the generated `.m3u`. */
    val discs: List<String> = emptyList(),
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
        if (platformId == "psx") ensurePsxBios(platform)
        else for (b in platform.bios) {
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
        val discs = item.media.firstOrNull { it.id == mediaId }?.discs.orEmpty().sortedBy { it.index }
        val rom: File
        val discLabels: List<String>
        if (discs.size > 1) {
            // Multi-disc set: download every disc, then play a local .m3u that lists them in order.
            val files = discs.mapIndexed { i, d ->
                downloadRom(d.mediaId, platform) { done, total ->
                    val pct = if (total > 0) (done * 100 / total).coerceIn(0, 100) else 0L
                    status(Step.ROM, i * 100L + pct, discs.size * 100L)
                }
            }
            rom = romCache.playlistFile(mediaId).also { m3u ->
                m3u.writeText(files.joinToString("\n", postfix = "\n") { romCache.relativeToPlaylistDir(it) })
            }
            discLabels = discs.mapIndexed { i, d -> d.label.ifBlank { "Disco ${i + 1}" } }
            romCache.trim(discs.map { it.mediaId }.toSet() + mediaId)
        } else {
            rom = downloadRom(mediaId, platform) { d, t -> status(Step.ROM, d, t) }
            discLabels = emptyList()
            romCache.trim(keepMediaId = mediaId)
        }

        // Saves
        status(Step.SAVE, 0, 0)
        val sram = api.sram(req.itemId, core)
        val state = if (resumeOk) api.state(wantedState!!.id) else null
        return Prepared(item, platform, core, rom, mediaId, sram, state, wantedState?.id, req.stateId != null && !resumeOk, saves, discLabels)
    }

    private fun downloadRom(mediaId: Long, platform: Platform, progress: (Long, Long) -> Unit): File =
        romCache.get(mediaId) ?: run {
            progress(0, 0)
            val tmp = romCache.target(mediaId, "download.tmp")
            val name = api.downloadRom(mediaId, extract = true, dest = tmp, progress = progress)
            val finalFile = File(tmp.parentFile, romFileName(name, platform))
            if (!tmp.renameTo(finalFile)) throw PlayException(PlayException.Kind.NETWORK, "rename")
            finalFile
        }

    /**
     * PS1 needs one of a few BIOS images (any one will do). Uses what is already in the system dir, else tries the
     * server's list, else (server that does not know `psx` yet) the usual names. The core wants lowercase names.
     */
    private fun ensurePsxBios(platform: Platform) {
        val listed = platform.bios.filter { Regex("""scph\d+\.bin""", RegexOption.IGNORE_CASE).matches(it.file) }
        val wanted = (PSX_BIOS + listed.map { it.file.lowercase() }).distinct()
        fun local(name: String) = File(systemDir, name)
        if (wanted.any { n -> local(n).let { it.isFile && it.length() > 0 } }) return
        var failure: ApiException? = null
        for (name in wanted) {
            val entry = listed.firstOrNull { it.file.equals(name, true) }
            if (platform.bios.isNotEmpty() && listed.isNotEmpty() && entry?.present != true) continue // server says it has no such file
            status(Step.BIOS, 0, 0)
            val dest = local(name)
            try {
                api.downloadBios(platform.id, entry?.file ?: name, dest) { d, t -> status(Step.BIOS, d, t) }
                if (dest.isFile && dest.length() > 0) return
            } catch (e: ApiException) {
                dest.delete()
                if (e.isAuth || e.isForbidden) throw e
                failure = e
            }
        }
        throw PlayException(PlayException.Kind.BIOS_MISSING, PSX_BIOS.first(), failure)
    }

    companion object {
        val PSX_BIOS = listOf("scph5501.bin", "scph1001.bin", "scph7001.bin")

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
            "psx" -> ".cue"; "md" -> ".md"; "sms" -> ".sms"; "gg" -> ".gg"; "pce" -> ".pce"; else -> ".bin"
        }

        fun md5(f: File): String {
            val md = MessageDigest.getInstance("MD5")
            f.inputStream().use { i -> val buf = ByteArray(64 * 1024); while (true) { val n = i.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
