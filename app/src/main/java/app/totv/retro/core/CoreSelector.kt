package app.totv.retro.core

import app.totv.retro.api.Platform

/** Why a core could not be chosen. */
sealed interface CoreChoice {
    data class Selected(val core: String, val reason: Reason) : CoreChoice
    data class None(val platform: String) : CoreChoice
}

enum class Reason { SERVER_PREFERENCE, BUILTIN_PREFERENCE, STATE_CORE }

/**
 * Pure core-selection logic.
 *
 * Order of preference for a platform:
 *  1. the core that wrote the state we want to resume (states are core specific), if it is embedded and
 *     supports the platform;
 *  2. the server's list `cores.android` (first = default), keeping only embedded cores;
 *  3. our built-in list for the platform.
 * On GBA the built-in ABI preference wins over the server list order (gpSP is the fast one on 32-bit ARM,
 * mGBA on 64-bit ARM), because only one of the two is embedded per ABI anyway.
 */
object CoreSelector {
    /** Built-in preference per platform (embedded cores only are ever returned). */
    val builtin: Map<String, List<String>> = mapOf(
        "nes" to listOf("fceumm"),
        "gb" to listOf("gambatte"),
        "gbc" to listOf("gambatte"),
        "gba" to listOf("mgba", "gpsp"),
        "sms" to listOf("smsplus"),
        "gg" to listOf("smsplus"),
        "pce" to listOf("mednafen_pce_fast"),
        "snes" to listOf("mednafen_supafaust"),
        "md" to listOf("blastem", "clownmdemu"),
        "psx" to listOf("pcsx_rearmed"),
    )

    /** GBA: per-ABI order. */
    private val abiOverride: Map<Pair<String, String>, List<String>> = mapOf(
        ("gba" to "armeabi-v7a") to listOf("gpsp", "mgba"),
        ("gba" to "arm64-v8a") to listOf("mgba", "gpsp"),
    )

    /** Platforms each core may be used for (guards `stateCore` and odd server lists). */
    val supports: Map<String, Set<String>> = mapOf(
        "fceumm" to setOf("nes"),
        "gambatte" to setOf("gb", "gbc"),
        "gpsp" to setOf("gba"),
        "mgba" to setOf("gba", "gb", "gbc"),
        "smsplus" to setOf("sms", "gg"),
        "mednafen_pce_fast" to setOf("pce"),
        "mednafen_supafaust" to setOf("snes"),
        "clownmdemu" to setOf("md"),
        "blastem" to setOf("md", "sms", "gg"),
        "pcsx_rearmed" to setOf("psx"),
    )

    fun select(
        platformId: String,
        serverCores: List<String>,
        embedded: Set<String>,
        abi: String,
        stateCore: String? = null,
    ): CoreChoice {
        fun ok(c: String) = c in embedded && supports[c]?.contains(platformId) == true
        if (!stateCore.isNullOrBlank() && ok(stateCore)) return CoreChoice.Selected(stateCore, Reason.STATE_CORE)

        val builtinOrder = abiOverride[platformId to abi] ?: builtin[platformId].orEmpty()
        val serverOrdered = serverCores.filter(::ok)
        // ABI override beats the server order when it names a core we embed.
        if (abiOverride.containsKey(platformId to abi)) {
            builtinOrder.firstOrNull(::ok)?.let { return CoreChoice.Selected(it, Reason.BUILTIN_PREFERENCE) }
        }
        serverOrdered.firstOrNull()?.let { return CoreChoice.Selected(it, Reason.SERVER_PREFERENCE) }
        builtinOrder.firstOrNull(::ok)?.let { return CoreChoice.Selected(it, Reason.BUILTIN_PREFERENCE) }
        return CoreChoice.None(platformId)
    }

    fun select(platform: Platform, embedded: Set<String>, abi: String, stateCore: String? = null) =
        select(platform.id, platform.cores.android, embedded, abi, stateCore)

    /** True when [stateCore] can be loaded by [chosenCore] (same core; the format is core specific). */
    fun canLoadState(chosenCore: String, stateCore: String): Boolean = chosenCore == stateCore
}
