package app.totv.retro.play

/**
 * Ring buffer of serialized emulator states for "Rebobinar" (LibretroDroid 0.14 has no native rewind).
 * Memory is capped at [capBytes]: the oldest snapshot goes first. Not thread safe on its own: the player
 * only touches it from one coroutine at a time.
 */
class RewindBuffer(private val capBytes: Int = DEFAULT_CAP, private val minFrames: Int = MIN_SNAPSHOTS) {
    private val items = ArrayDeque<ByteArray>()
    private var bytes = 0

    val size get() = items.size
    val isEmpty get() = items.isEmpty()

    /** False when a single state is so large that fewer than [minFrames] would fit (e.g. PS1). */
    fun fits(stateSize: Int) = stateSize in 1..(capBytes / minFrames)

    fun push(state: ByteArray): Boolean {
        if (!fits(state.size)) return false
        items.addLast(state); bytes += state.size
        while (bytes > capBytes && items.size > 1) bytes -= items.removeFirst().size
        return true
    }

    /** The newest snapshot, removed; null when empty. */
    fun pop(): ByteArray? = items.removeLastOrNull()?.also { bytes -= it.size }

    fun clear() { items.clear(); bytes = 0 }

    companion object {
        const val DEFAULT_CAP = 32 * 1024 * 1024
        const val MIN_SNAPSHOTS = 16
        /** Snapshot every 100 ms of play (about 6 frames), so 32 MB of small states is many seconds. */
        const val INTERVAL_MS = 100L
        /** Step back one snapshot this often while the button is held. */
        const val STEP_MS = 60L

        /** Default per platform: on for 8/16-bit machines, off for PS1 (and anything unknown). */
        fun defaultOn(platform: String) = platform in setOf("nes", "snes", "gb", "gbc", "gba", "md", "sms", "gg", "pce")
    }
}
