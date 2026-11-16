package app.totv.retro.play

import android.content.Intent

const val ACTION_PLAY = "app.totv.retro.action.PLAY"
const val EXTRA_PLAYED_MS = "playedMs"

/** The extras of `app.totv.retro.action.PLAY` (see docs/games-api.md of the Totv server). */
data class PlayRequest(
    val server: String,
    val token: String,
    val itemId: Long,
    val mediaId: Long?,
    val stateId: Long?,
    val title: String,
    val platform: String?,
    val userName: String,
) {
    companion object {
        /** Reads ids that may arrive as Long, Int or String. */
        private fun Intent.longExtra(key: String): Long? {
            val v = extras?.get(key) ?: return null
            return when (v) {
                is Long -> v
                is Int -> v.toLong()
                is String -> v.trim().toLongOrNull()
                else -> null
            }?.takeIf { it > 0 }
        }

        fun from(intent: Intent): PlayRequest? {
            if (intent.action != ACTION_PLAY) return null
            val server = intent.getStringExtra("server")?.takeIf { it.isNotBlank() } ?: return null
            val token = intent.getStringExtra("token")?.takeIf { it.isNotBlank() } ?: return null
            val itemId = intent.longExtra("itemId") ?: return null
            return PlayRequest(
                server = server,
                token = token,
                itemId = itemId,
                mediaId = intent.longExtra("mediaId"),
                stateId = intent.longExtra("stateId"),
                title = intent.getStringExtra("title").orEmpty(),
                platform = intent.getStringExtra("platform")?.takeIf { it.isNotBlank() },
                userName = intent.getStringExtra("userName").orEmpty(),
            )
        }
    }
}
