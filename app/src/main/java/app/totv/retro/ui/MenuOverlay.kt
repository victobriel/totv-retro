package app.totv.retro.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import app.totv.retro.api.SaveState
import app.totv.retro.api.Saves
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** What the in-game menu needs from the player. */
interface MenuHost {
    val currentCore: String
    val fast: Boolean
    val stretch: Boolean
    val sharp: Boolean
    val hasGamepad: Boolean
    fun toggleFast()
    fun toggleStretch()
    fun toggleSharp()
    fun resumeGame()
    fun restartGame()
    fun exitGame()
    fun saveToSlot(slot: Int)
    fun loadFromState(state: SaveState)
    suspend fun fetchSaves(): Saves?
    suspend fun fetchThumb(url: String): ByteArray?
}

/**
 * The in-game menu: a side panel docked right over a scrim. Panels stack; Back closes the innermost layer
 * and, from the main panel, the menu itself. Focus is trapped inside (the game view is not focusable).
 */
class MenuOverlay(context: Context, private val scope: CoroutineScope, private val host: MenuHost) : FrameLayout(context) {
    private class Layer(val view: View, var lastFocus: View? = null)

    private val stack = ArrayList<Layer>()
    private val holder = FrameLayout(context)
    private var thumbJob: Job? = null
    private val thumbCache = HashMap<Long, Bitmap>()

    init {
        setBackgroundColor(C.scrim60)
        visibility = GONE
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        addView(holder, LayoutParams(context.dp(Layout.sidePanelW), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END).apply {
            setMargins(0, context.dp(Sp.l), context.dp(Layout.gutter), context.dp(Sp.l))
        })
    }

    val isOpen get() = visibility == VISIBLE

    fun open() {
        stack.clear(); holder.removeAllViews()
        visibility = VISIBLE
        push(mainPanel())
    }

    fun close() {
        thumbJob?.cancel()
        visibility = GONE
        stack.clear(); holder.removeAllViews()
    }

    /** Back: closes the innermost layer. Returns true when the key was consumed. */
    fun back(): Boolean {
        if (!isOpen) return false
        if (stack.size > 1) { pop(); return true }
        host.resumeGame()
        return true
    }

    private fun push(panel: SidePanel, focus: View? = null) {
        stack.lastOrNull()?.lastFocus = findFocus()
        stack.add(Layer(panel))
        holder.removeAllViews()
        holder.addView(panel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
        panel.post { panel.focusFirst(focus) }
    }

    private fun pop() {
        thumbJob?.cancel()
        stack.removeAt(stack.lastIndex)
        val top = stack.last()
        holder.removeAllViews()
        holder.addView(top.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
        top.view.post { (top.lastFocus ?: (top.view as SidePanel).firstFocusable())?.requestFocus() }
    }

    private fun row(title: String, subtitle: String? = null, trailing: String? = null, selected: Boolean = false, thumb: Boolean = false, onClick: () -> Unit = {}) =
        RowView(context, title, subtitle, trailing, selected, thumb).also { r -> r.setOnClickListener { onClick() } }

    // ---- panels -------------------------------------------------------------------------------

    private fun mainPanel(): SidePanel {
        val p = SidePanel(context, Tx.menuTitle)
        val first = p.add(row(Tx.resume) { host.resumeGame() })
        p.add(row(Tx.saveState, trailing = "›") { push(slotsPanel(save = true)) })
        p.add(row(Tx.loadState, trailing = "›") { push(slotsPanel(save = false)) })
        p.add(row(Tx.restart) { push(confirmPanel(Tx.confirmRestartTitle, Tx.confirmRestartBody, Tx.restart) { host.restartGame() }) })
        val ff = row(Tx.fastForward, trailing = onOff(host.fast)) {}
        ff.setOnClickListener { host.toggleFast(); ff.setTrailing(onOff(host.fast)) }
        p.add(ff)
        val asp = row(Tx.aspect, trailing = aspectName()) {}
        asp.setOnClickListener { host.toggleStretch(); asp.setTrailing(aspectName()) }
        p.add(asp)
        val flt = row(Tx.filter, trailing = filterName()) {}
        flt.setOnClickListener { host.toggleSharp(); flt.setTrailing(filterName()) }
        p.add(flt)
        p.add(row(Tx.controls, trailing = "›") { push(controlsPanel()) })
        p.add(row(Tx.exit) { host.exitGame() })
        p.post { first.requestFocus() }
        return p
    }

    private fun onOff(v: Boolean) = if (v) Tx.on else Tx.off
    private fun aspectName() = if (host.stretch) Tx.aspectStretch else Tx.aspectOriginal
    private fun filterName() = if (host.sharp) Tx.filterSharp else Tx.filterSoft

    private fun controlsPanel(): SidePanel {
        val p = SidePanel(context, Tx.controlsTitle, if (host.hasGamepad) null else Tx.gamepadHint)
        p.section(Tx.sectionGamepad)
        Tx.controlsGamepad.forEach { (k, v) -> p.add(row(k, subtitle = v).also { it.setRowEnabled(false) }) }
        p.section(Tx.sectionRemote)
        Tx.controlsRemote.forEach { (k, v) -> p.add(row(k, subtitle = v).also { it.setRowEnabled(false) }) }
        val back = p.add(row(Tx.back) { back() }, Sp.s)
        p.post { back.requestFocus() }
        return p
    }

    /** Destructive confirmations focus Cancel first (design system rule). */
    private fun confirmPanel(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit): SidePanel {
        val p = SidePanel(context, title, body)
        val cancel = p.add(ButtonView(context, Tx.cancel).also { it.setOnClickListener { back() } })
        p.add(ButtonView(context, confirmLabel, primary = true).also { it.setOnClickListener { onConfirm() } }, Sp.xs)
        p.post { cancel.requestFocus() }
        return p
    }

    private fun slotsPanel(save: Boolean): SidePanel {
        val p = SidePanel(context, if (save) Tx.saveState else Tx.loadState)
        val loading = p.add(context.text(Tx.loading, Type.meta, C.textMuted).apply { setPadding(context.dp(Sp.sm), context.dp(Sp.xs), 0, 0) })
        thumbJob?.cancel()
        thumbJob = scope.launch {
            val saves = host.fetchSaves()
            p.list.removeView(loading)
            val states = saves?.states.orEmpty()
            var firstRow: View? = null
            val rowsWithThumb = ArrayList<Pair<RowView, SaveState>>()
            if (!save) {
                val autos = states.filter { it.auto }.sortedByDescending { it.createdAt }
                autos.firstOrNull()?.let { s ->
                    val r = stateRow(Tx.autoState, s, save = false)
                    p.add(r); rowsWithThumb += r to s; firstRow = firstRow ?: r
                }
            }
            for (slot in 1..9) {
                val s = states.firstOrNull { !it.auto && it.slot == slot.toLong() }
                if (!save && s == null) continue
                val r = if (s != null) stateRow("${Tx.slot} $slot", s, save, slot) else row("${Tx.slot} $slot", subtitle = Tx.emptySlot, thumb = true) { host.saveToSlot(slot) }
                p.add(r)
                if (s != null) rowsWithThumb += (r as RowView) to s
                firstRow = firstRow ?: r
            }
            if (!save && firstRow == null) p.add(context.text(Tx.noStates, Type.meta, C.textMuted).apply { setPadding(context.dp(Sp.sm), context.dp(Sp.xs), 0, 0) })
            val back = p.add(row(Tx.back) { back() }, Sp.s)
            (firstRow ?: back).requestFocus()
            for ((r, s) in rowsWithThumb) {
                val url = s.thumbUrl ?: continue
                val bmp = thumbCache[s.id] ?: bitmapOrNull(host.fetchThumb(url))?.also { thumbCache[s.id] = it }
                if (bmp != null) r.thumb?.setImageBitmap(bmp)
            }
        }
        return p
    }

    private fun stateRow(title: String, s: SaveState, save: Boolean, slot: Int = 0): RowView {
        val ok = save || host.currentCore == s.core
        val sub = fmtDate(s.createdAt) + if (!ok) "  •  " + Tx.wrongCore else ""
        val r = row(title, subtitle = sub, thumb = true) { if (save) confirmOverwrite(slot) else host.loadFromState(s) }
        r.setRowEnabled(ok)
        return r
    }

    private fun confirmOverwrite(slot: Int) {
        push(confirmPanel(Tx.confirmOverwriteTitle, "${Tx.slot} $slot", Tx.saveState) { host.saveToSlot(slot) })
    }

    private fun fmtDate(t: Long): String {
        if (t <= 0) return ""
        val ms = if (t < 100_000_000_000L) t * 1000 else t
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ms))
    }

    /** Called by the host after a slot save so a re-opened list shows the new state. */
    fun clearThumbs() { thumbCache.clear() }
}
