package app.totv.retro.play

import com.swordfish.libretrodroid.ShaderConfig

/** The shader configs LibretroDroid 0.14 ships (Default, Sharp, CRT, LCD, CUT upscaler), as a menu option. */
enum class VideoFilter(val id: String, val label: String) {
    SHARP("sharp", "Nítido"),
    SMOOTH("smooth", "Suave"),
    CRT("crt", "CRT"),
    LCD("lcd", "LCD"),
    PIXEL("pixel", "Pixel art (CUT)");

    fun config(): ShaderConfig = when (this) {
        SHARP -> ShaderConfig.Sharp
        SMOOTH -> ShaderConfig.Default
        CRT -> ShaderConfig.CRT
        LCD -> ShaderConfig.LCD
        PIXEL -> ShaderConfig.CUT3()
    }

    fun next() = entries[(ordinal + 1) % entries.size]

    companion object {
        fun parse(id: String?) = entries.firstOrNull { it.id == id }
        /** 3D / high-res platforms look better filtered; 8/16-bit default to sharp pixels. */
        fun defaultFor(platform: String) = if (platform == "psx") SMOOTH else SHARP
    }
}
