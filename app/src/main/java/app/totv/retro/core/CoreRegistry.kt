package app.totv.retro.core

import android.content.Context
import android.os.Build
import java.io.File

/** Finds the cores embedded in the APK (`lib<core>.so` in the native library dir). */
object CoreRegistry {
    fun libFile(context: Context, core: String) = File(context.applicationInfo.nativeLibraryDir, "lib$core.so")

    fun embedded(context: Context): Set<String> =
        CoreSelector.supports.keys.filter { libFile(context, it).isFile }.toSet()

    /** Primary ABI of this device that our APK actually ships. */
    fun abi(): String = Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "armeabi-v7a" } ?: Build.SUPPORTED_ABIS.firstOrNull() ?: ""

    /** Native display aspect ratio (w/h) of a platform, used by the "Esticar" option. */
    fun displayAspect(platform: String): Float = when (platform) {
        "gb", "gbc", "gg" -> 10f / 9f
        "gba" -> 3f / 2f
        "snes" -> 8f / 7f
        "nes" -> 8f / 7f
        else -> 4f / 3f
    }
}
