package app.totv.retro.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

/*
 * Small view toolkit for the player screens. TV focus rules (same intent as the Totv design system):
 * every focusable shows focus without relying on colour alone: rows turn white with black content,
 * buttons turn lime with black content and scale up a little.
 */

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Int): Float = v * resources.displayMetrics.density

fun rounded(color: Int, radiusDp: Int, ctx: Context): GradientDrawable =
    GradientDrawable().apply { setColor(color); cornerRadius = ctx.dpf(radiusDp) }

fun Context.text(
    text: CharSequence,
    size: Float = Type.body,
    color: Int = C.text,
    bold: Boolean = false,
    gravity: Int = Gravity.START,
): TextView = TextView(this).apply {
    this.text = text
    textSize = size
    setTextColor(color)
    this.gravity = gravity
    if (bold) setTypeface(typeface, Typeface.BOLD)
    includeFontPadding = false
}

/** A focusable row: [leading] mark, title (+ optional subtitle), optional thumbnail, trailing value. */
class RowView(context: Context, title: String, subtitle: String? = null, trailing: String? = null, selected: Boolean = false, withThumb: Boolean = false) :
    LinearLayout(context) {
    private val titleView = context.text(title, Type.bodyS, C.text, bold = true)
    private val subView = subtitle?.let { context.text(it, Type.micro, C.textSecondary) }
    private val trailView = context.text(trailing.orEmpty(), Type.meta, C.textSecondary)
    private val markView = context.text(if (selected) "✓" else "", Type.bodyS, C.lime, bold = true)
    val thumb: ImageView? = if (withThumb) ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = rounded(C.control, R8.xs, context) } else null
    private var enabledRow = true

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        val h = if (withThumb) Ctl.rowThumbH else if (subtitle != null) Ctl.row2H else Ctl.rowH
        minimumHeight = context.dp(h)
        setPadding(context.dp(Sp.sm), context.dp(Sp.xxs), context.dp(Sp.sm), context.dp(Sp.xxs))
        addView(markView, LayoutParams(context.dp(Sp.m), ViewGroup.LayoutParams.WRAP_CONTENT))
        if (thumb != null) addView(thumb, LayoutParams(context.dp(96), context.dp(Ctl.rowThumbH - 12)).apply { marginEnd = context.dp(Sp.sm) })
        val col = LinearLayout(context).apply { orientation = VERTICAL }
        col.addView(titleView)
        subView?.let { col.addView(it) }
        addView(col, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(trailView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = context.dp(Sp.s) })
        applyFocus(false)
        setOnFocusChangeListener { _, f -> applyFocus(f) }
    }

    fun setTrailing(t: String) { trailView.text = t }
    fun setSubtitle(t: String) { subView?.text = t }

    /** Disabled rows are not focusable (design system rule). */
    fun setRowEnabled(on: Boolean) {
        enabledRow = on
        isFocusable = on
        isClickable = on
        applyFocus(hasFocus())
    }

    private fun applyFocus(f: Boolean) {
        background = if (f) rounded(C.text, R8.s, context) else null
        val main = if (f) C.black else if (enabledRow) C.text else C.textDisabled
        val sec = if (f) Color.argb(0xa6, 0, 0, 0) else if (enabledRow) C.textSecondary else C.textDisabled
        titleView.setTextColor(main)
        subView?.setTextColor(sec)
        trailView.setTextColor(sec)
        markView.setTextColor(if (f) C.black else C.lime)
    }
}

/** Button: secondary look at rest, lime fill + black text + scale 1.04 on focus. */
class ButtonView(context: Context, label: String, primary: Boolean = false) : TextView(context) {
    private val primaryLook = primary

    init {
        text = label
        textSize = Type.body
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        isFocusable = true
        isClickable = true
        minHeight = context.dp(Ctl.m)
        minimumHeight = context.dp(Ctl.m)
        setPadding(context.dp(Sp.m), 0, context.dp(Sp.m), 0)
        apply(false)
        setOnFocusChangeListener { _, f -> apply(f) }
    }

    private fun apply(f: Boolean) {
        val bg = when {
            f -> C.lime
            primaryLook -> C.text
            else -> C.control
        }
        background = rounded(bg, R8.s, context)
        setTextColor(if (f || primaryLook) C.black else C.text)
        animate().scaleX(if (f) Focus.scaleButton else 1f).scaleY(if (f) Focus.scaleButton else 1f).setDuration(Motion.fast).start()
    }
}

/** Panel docked to the right (width 440dp): title, scrollable rows. Back is handled by the owner. */
class SidePanel(context: Context, title: String, note: String? = null) : LinearLayout(context) {
    val list = LinearLayout(context).apply { orientation = VERTICAL }
    private val scroll = ScrollView(context).apply { isFillViewport = false; isVerticalScrollBarEnabled = false; addView(list) }

    init {
        orientation = VERTICAL
        background = rounded(C.raised, R8.m, context)
        setPadding(context.dp(Sp.s), context.dp(Sp.s), context.dp(Sp.s), context.dp(Sp.s))
        addView(context.text(title, Type.h5, C.text, bold = true).apply { setPadding(context.dp(Sp.sm), context.dp(Sp.xs), 0, context.dp(Sp.sm)) })
        if (note != null) addView(context.text(note, Type.caption, C.textMuted).apply { setPadding(context.dp(Sp.sm), 0, context.dp(Sp.sm), context.dp(Sp.sm)) })
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun add(v: View, topMargin: Int = 0): View {
        list.addView(v, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { this.topMargin = context.dp(topMargin) })
        return v
    }

    fun section(title: String) = add(context.text(title.uppercase(), Type.caption, C.textMuted, bold = true).apply {
        setPadding(context.dp(Sp.sm), context.dp(Sp.s), 0, context.dp(Sp.xxs))
    })

    fun firstFocusable(): View? {
        for (i in 0 until list.childCount) { val c = list.getChildAt(i); if (c.isFocusable) return c }
        return null
    }

    fun focusFirst(preferred: View? = null) { (preferred ?: firstFocusable())?.requestFocus() }
}

/** Full-screen status: spinner or error, title, detail, optional progress bar and actions. */
class StatusView(context: Context) : FrameLayout(context) {
    private val spinner = ProgressBar(context).apply {
        isIndeterminate = true
        indeterminateTintList = android.content.res.ColorStateList.valueOf(C.lime)
    }
    private val title = context.text("", Type.h2, C.text, bold = true, gravity = Gravity.CENTER)
    private val detail = context.text("", Type.bodyL, C.textSecondary, gravity = Gravity.CENTER)
    private val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000
        progressTintList = android.content.res.ColorStateList.valueOf(C.lime)
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(C.track)
        visibility = GONE
    }
    private val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }

    init {
        setBackgroundColor(C.bg)
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        col.addView(spinner, LinearLayout.LayoutParams(context.dp(40), context.dp(40)))
        col.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(Sp.m) })
        col.addView(detail, LinearLayout.LayoutParams(context.dp(560), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(Sp.xs) })
        col.addView(bar, LinearLayout.LayoutParams(context.dp(360), context.dp(4)).apply { topMargin = context.dp(Sp.m) })
        col.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(Sp.l) })
        addView(col, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    fun showLoading(titleText: String, detailText: String = "") {
        spinner.visibility = VISIBLE
        title.text = titleText
        detail.text = detailText
        detail.visibility = if (detailText.isEmpty()) GONE else VISIBLE
        actions.removeAllViews()
        bar.visibility = GONE
        visibility = VISIBLE
    }

    fun setProgress(done: Long, total: Long) {
        if (total > 0) { bar.visibility = VISIBLE; bar.progress = (1000.0 * done / total).toInt().coerceIn(0, 1000) } else bar.visibility = GONE
    }

    fun showError(message: String, vararg buttons: ButtonView) {
        spinner.visibility = GONE
        bar.visibility = GONE
        title.text = Tx.errTitle
        detail.text = message
        detail.visibility = VISIBLE
        actions.removeAllViews()
        buttons.forEachIndexed { i, b -> actions.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (i > 0) marginStart = context.dp(Sp.sm) }) }
        visibility = VISIBLE
        buttons.firstOrNull()?.requestFocus()
    }
}

fun bitmapOrNull(bytes: ByteArray?): Bitmap? = bytes?.let { runCatching { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
