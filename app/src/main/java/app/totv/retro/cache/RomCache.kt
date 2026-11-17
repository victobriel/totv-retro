package app.totv.retro.cache

import java.io.File

/**
 * ROM cache in the app cache dir, one directory per media id (`roms/<mediaId>/<file name>`), evicted
 * least-recently-used first when the total passes [limitBytes] (default 1 GB). Pure java.io: unit tested.
 */
class RomCache(private val root: File, private val limitBytes: Long = 1L shl 30) {
    private fun dir(mediaId: Long) = File(root, mediaId.toString())

    /** The cached ROM for [mediaId] (and marks it as just used), or null. */
    fun get(mediaId: Long): File? {
        val all = dir(mediaId).listFiles { x -> x.isFile && !x.name.endsWith(".part") } ?: return null
        // a disc dump unpacked from a bundle has the .cue sheet next to its tracks: the sheet is the entry point
        val f = all.firstOrNull { it.name.endsWith(".cue", ignoreCase = true) } ?: all.firstOrNull() ?: return null
        if (f.length() <= 0) return null
        f.setLastModified(System.currentTimeMillis())
        dir(mediaId).setLastModified(System.currentTimeMillis())
        return f
    }

    /** Where to write a new download of [mediaId] named [name] (sanitised). */
    fun target(mediaId: Long, name: String): File {
        dir(mediaId).deleteRecursively()
        val safe = name.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_").take(120).ifBlank { "rom.bin" }
        return File(dir(mediaId), safe).also { it.parentFile?.mkdirs() }
    }

    /** Deletes least-recently-used entries until under the limit, never touching [keepMediaId]. */
    fun trim(keepMediaId: Long? = null) = trim(setOfNotNull(keepMediaId))

    /** Where the generated `.m3u` of a multi-disc set lives (outside the per-media dirs; never evicted). */
    fun playlistFile(mediaId: Long): File = File(File(root, PLAYLIST_DIR).apply { mkdirs() }, "$mediaId.m3u")

    fun trim(keep: Set<Long>) {
        val entries = root.listFiles { f -> f.isDirectory && !f.name.startsWith("_") }?.map { it to sizeOf(it) } ?: return
        var total = entries.sumOf { it.second }
        for ((d, size) in entries.sortedBy { it.first.lastModified() }) {
            if (total <= limitBytes) break
            if (d.name.toLongOrNull() in keep) continue
            d.deleteRecursively()
            total -= size
        }
    }

    /** Path of [file] (inside a media dir) as seen from the playlist dir, for `.m3u` lines. */
    fun relativeToPlaylistDir(file: File): String = file.relativeTo(File(root, PLAYLIST_DIR)).path.replace(File.separatorChar, '/')

    fun totalSize(): Long = sizeOf(root)

    private fun sizeOf(f: File): Long = if (f.isFile) f.length() else f.listFiles()?.sumOf { sizeOf(it) } ?: 0L

    private companion object { const val PLAYLIST_DIR = "_m3u" }
}
