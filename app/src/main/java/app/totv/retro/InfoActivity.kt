package app.totv.retro

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import app.totv.retro.ui.C
import app.totv.retro.ui.Layout
import app.totv.retro.ui.Sp
import app.totv.retro.ui.Tx
import app.totv.retro.ui.Type
import app.totv.retro.ui.dp
import app.totv.retro.ui.text

/** Launcher screen: games are opened from the Totv app, so this only says so. */
class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(Layout.gutter), 0, dp(Layout.gutter), 0)
        }
        col.addView(text(Tx.appName.uppercase(), Type.h5, C.lime, bold = true, gravity = Gravity.CENTER))
        col.addView(text(Tx.infoTitle, Type.h1, C.text, bold = true, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(Sp.s) })
        col.addView(text(Tx.infoBody, Type.bodyL, C.textSecondary, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(dp(640), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(Sp.s) })
        col.addView(text(Tx.infoHint, Type.meta, C.textMuted, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(dp(640), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(Sp.m) })
        val root = FrameLayout(this).apply { setBackgroundColor(C.bg) }
        root.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        setContentView(root)
    }
}
