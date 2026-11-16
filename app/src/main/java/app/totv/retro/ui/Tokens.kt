package app.totv.retro.ui

import android.graphics.Color

/**
 * Visual constants of the Totv family (dark surfaces, lime accent, 8-point spacing, 960x540 dp TV canvas).
 * They are re-implemented here on purpose: this app is an independent GPL-3.0 project and does not share code
 * with the Totv apps, only the look.
 */
object C {
    val lime = Color.parseColor("#e0ff00")
    val bg = Color.parseColor("#0b0b0b")
    val surface = Color.parseColor("#141414")
    val raised = Color.parseColor("#1c1c1c")
    val control = Color.argb(0x14, 255, 255, 255)        // white 8%
    val controlStrong = Color.argb(0x29, 255, 255, 255)  // white 16%
    val track = Color.argb(0x33, 255, 255, 255)          // white 20%
    val border = Color.argb(0x1a, 255, 255, 255)         // white 10%
    val text = Color.WHITE
    val textSecondary = Color.argb(0xbd, 255, 255, 255)  // 74%
    val textMuted = Color.parseColor("#8c8c8c")
    val textDisabled = Color.argb(0x47, 255, 255, 255)   // 28%
    val black = Color.BLACK
    val error = Color.parseColor("#ff2222")
    val success = Color.parseColor("#50c24e")
    val overlay = Color.argb(0xb0, 0, 0, 0)
    val scrim60 = Color.argb(0x99, 0, 0, 0)
}

/** Spacing (dp) on the 8-point grid. */
object Sp { const val xxs = 4; const val xs = 8; const val sm = 12; const val s = 16; const val m = 24; const val l = 32; const val xl = 40; const val xxl = 48 }

/** Corner radii (dp). */
object R8 { const val xs = 4; const val s = 8; const val m = 12; const val l = 16; const val pill = 999 }

/** Control heights (dp, TV values) and layout widths. */
object Ctl { const val s = 36; const val m = 48; const val rowH = 44; const val row2H = 58; const val rowThumbH = 72 }
object Layout { const val gutter = 48; const val sidePanelW = 440; const val dialogW = 520 }

/** Focus (TV): white fill with black content on rows, lime fill on buttons, 3dp white ring on artwork. */
object Focus { const val ringWidth = 3; const val ringOffset = 4; const val scaleButton = 1.04f; const val scaleCard = 1.05f }

/** Type sizes (sp, TV). */
object Type { const val h1 = 32f; const val h2 = 28f; const val h3 = 24f; const val h5 = 18f; const val bodyL = 17f; const val body = 15f; const val bodyS = 14f; const val meta = 13f; const val caption = 12f; const val micro = 11f }

/** Motion (ms). */
object Motion { const val fast = 160L; const val base = 240L; const val fade = 500L }
