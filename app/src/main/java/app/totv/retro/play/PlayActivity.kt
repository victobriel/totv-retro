package app.totv.retro.play

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.totv.retro.BuildConfig
import app.totv.retro.api.SaveState
import app.totv.retro.api.Saves
import app.totv.retro.api.TotvApi
import app.totv.retro.cache.RomCache
import app.totv.retro.core.CoreRegistry
import app.totv.retro.input.PadMapper
import app.totv.retro.ui.ButtonView
import app.totv.retro.ui.MenuHost
import app.totv.retro.ui.MenuOverlay
import app.totv.retro.ui.StatusView
import app.totv.retro.ui.Tx
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.LibretroDroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Uploads outlive the activity: they run on this scope, one at a time. */
private object AppScope {
    val io = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
}

private class Snapshot(val state: ByteArray?, val sram: ByteArray?, val thumb: ByteArray?)

class PlayActivity : AppCompatActivity(), MenuHost {
    private lateinit var root: FrameLayout
    private lateinit var status: StatusView
    private lateinit var menu: MenuOverlay
    private val main = Handler(Looper.getMainLooper())

    private var req: PlayRequest? = null
    private var api: TotvApi? = null
    private var prepared: Prepared? = null
    private var view: GLRetroView? = null
    private var gameRunning = false
    private var exiting = false
    private var sessionStopped = false
    private var lastSram: ByteArray? = null
    private var lastAutosaveAt = 0L
    private var lastSentAxisX = 0f
    private var lastSentAxisY = 0f
    private var selectDown = false
    private var startDown = false
    private var chordConsumed = false
    private var backDownHandled = false

    // play time (only while resumed, started and menu closed)
    private var playedAcc = 0L
    private var runStart = 0L
    private var resumed = false

    private val prefs by lazy { getSharedPreferences("retro", Context.MODE_PRIVATE) }
    override var stretch = false
    private var filter = VideoFilter.SHARP
    override val filterLabel get() = filter.label
    override var rewind = false
    override val discs get() = prepared?.discs.orEmpty()
    override var currentDisc = 0
    private val rewindBuf = RewindBuffer()
    @Volatile private var rewinding = false
    private var rewindKeyDown = false
    private var rewindAxisDown = false
    override var fast = false
    override val currentCore get() = prepared?.core.orEmpty()
    override val hasGamepad get() = InputDevice.getDeviceIds().any { id ->
        InputDevice.getDevice(id)?.let { it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD } == true
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ---- lifecycle -------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stretch = prefs.getBoolean("stretch", false)
        root = FrameLayout(this).apply { setBackgroundColor(app.totv.retro.ui.C.black) }
        status = StatusView(this)
        menu = MenuOverlay(this, lifecycleScope, this)
        root.addView(status, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(menu, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        hideSystemBars()

        lifecycle.addObserver(LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> { resumed = true; updateClock() }
                Lifecycle.Event.ON_DESTROY -> stopSessionOnce()
                else -> {}
            }
        })

        val r = PlayRequest.from(intent)
        if (r == null) { fail(Tx.errBadIntent); return }
        req = r
        api = TotvApi(r.server, r.token, http)
        boot(r)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A new PLAY intent while a game is running: ignore the second one, keep the current game.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onPause() {
        resumed = false
        updateClock()
        super.onPause()
    }

    // ---- boot ------------------------------------------------------------------------------------

    private fun boot(r: PlayRequest) {
        status.showLoading(Tx.loading, r.title)
        val systemDir = File(filesDir, "system").apply { mkdirs() }
        val preparer = Preparer(
            api!!, RomCache(File(cacheDir, "roms")), systemDir, CoreRegistry.embedded(this), CoreRegistry.abi(),
        ) { step, done, total ->
            main.post {
                val t = when (step) {
                    Preparer.Step.SERVER -> Tx.loadingPlatforms
                    Preparer.Step.BIOS -> Tx.loadingBios
                    Preparer.Step.ROM -> Tx.loadingRom
                    Preparer.Step.SAVE -> Tx.loadingSave
                }
                status.showLoading(t, r.title)
                status.setProgress(done, total)
            }
        }
        lifecycleScope.launch {
            val p = try {
                withContext(Dispatchers.IO) { preparer.prepare(r) }
            } catch (e: PlayException) {
                fail(describe(e)); return@launch
            } catch (e: Exception) {
                fail(Tx.errNetwork); return@launch
            }
            prepared = p
            lastSram = p.sram
            startEmulation(p)
        }
    }

    private fun describe(e: PlayException) = when (e.kind) {
        PlayException.Kind.BAD_REQUEST, PlayException.Kind.NO_MEDIA -> Tx.errBadIntent
        PlayException.Kind.NETWORK -> Tx.errNetwork
        PlayException.Kind.AUTH -> Tx.errAuth
        PlayException.Kind.NOT_FOUND -> Tx.errNotFound
        PlayException.Kind.SCHEDULE -> Tx.errSchedule
        PlayException.Kind.NO_CORE -> "${Tx.errNoCore} (${e.detail})."
        PlayException.Kind.BIOS_MISSING -> "${Tx.errBios}: ${e.detail}."
    }

    private fun fail(message: String) {
        val back = ButtonView(this, Tx.back, primary = true).also { it.setOnClickListener { finishWith(Activity.RESULT_CANCELED) } }
        val retry = req?.let { r -> ButtonView(this, Tx.retry).also { b -> b.setOnClickListener { boot(r) } } }
        status.showError(message, *listOfNotNull(back, retry).toTypedArray())
    }

    // ---- emulation --------------------------------------------------------------------------------

    private fun startEmulation(p: Prepared) {
        val pid = p.platform.id
        filter = VideoFilter.parse(prefs.getString("filter_$pid", null)) ?: VideoFilter.defaultFor(pid)
        rewind = prefs.getBoolean("rewind_$pid", RewindBuffer.defaultOn(pid))
        status.showLoading(Tx.starting, req?.title.orEmpty())
        val savesDir = File(filesDir, "saves").apply { mkdirs() }
        val data = GLRetroViewData(this).apply {
            coreFilePath = CoreRegistry.libFile(this@PlayActivity, p.core).absolutePath
            gameFilePath = p.rom.absolutePath
            systemDirectory = File(filesDir, "system").absolutePath
            savesDirectory = savesDir.absolutePath
            saveRAMState = p.sram
            shader = filter.config()
            rumbleEventsEnabled = false
            preferLowLatencyAudio = true
        }
        val v = GLRetroView(this, data)
        v.isFocusable = false
        v.isFocusableInTouchMode = false
        view = v
        root.addView(v, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        lifecycle.addObserver(v)

        lifecycleScope.launch {
            v.getGLRetroErrors().collect { code ->
                if (!gameRunning) fail("${Tx.errCore} (código $code)") else toast(Tx.errCore)
            }
        }
        lifecycleScope.launch {
            v.getGLRetroEvents().collect { ev ->
                if (ev == GLRetroView.GLRetroEvents.SurfaceCreated && !gameRunning) onGameLoaded(v, p)
            }
        }
        v.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyAspect() }
    }

    private fun onGameLoaded(v: GLRetroView, p: Prepared) {
        gameRunning = true
        status.visibility = View.GONE
        applyAspect()
        // Our snapshot observer is added now, i.e. after LibretroDroid's own render observer, so on pause
        // it runs first (observers get downward events in reverse order): the GL thread is still alive.
        lifecycle.addObserver(LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) onLeaving()
        })
        updateClock()
        if (p.resumeSkipped) toast(Tx.wrongCore)
        if (!hasGamepad) main.postDelayed({ toast(Tx.gamepadHint, long = true) }, 1500)
        lifecycleScope.launch {
            p.resumeState?.let { bytes ->
                val ok = withContext(Dispatchers.Default) { runCatching { v.unserializeState(bytes) }.getOrDefault(false) }
                if (!ok) toast(Tx.loadFailed)
            }
        }
        startSession()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launchLoops()
            }
        }
    }

    private suspend fun launchLoops() = coroutineScope2 {
        launch { // heartbeat
            while (true) {
                delay(HEARTBEAT_MS)
                sendSession("heartbeat")
            }
        }
        launch { // SRAM every 60 s if changed
            while (true) {
                delay(SRAM_MS)
                val s = withContext(Dispatchers.Default) { timed(3000) { view?.serializeSRAM() } }
                if (s != null) uploadSramIfChanged(s)
            }
        }
        launch { rewindLoop() }
        launch { // autosave every 5 min
            while (true) {
                delay(AUTOSAVE_MS)
                val snap = captureSnapshot(thumb = true)
                uploadAuto(snap)
            }
        }
    }

    /**
     * "Rebobinar": LibretroDroid has no native rewind, so keep a ring of serialized states (every
     * [RewindBuffer.INTERVAL_MS], 32 MB cap) and, while L2 is held, step back through them. Platforms whose
     * state does not fit (PS1) never record anything.
     */
    private suspend fun rewindLoop() {
        var tooBig = false
        while (true) {
            val v = view
            if (v == null || !rewind || tooBig || menu.isOpen) {
                if (!rewind) { rewindBuf.clear(); setRewinding(false) }
                delay(200); continue
            }
            if (rewinding) {
                val s = rewindBuf.pop()
                if (s != null) withContext(Dispatchers.Default) { runCatching { v.unserializeState(s) } }
                delay(RewindBuffer.STEP_MS)
            } else {
                val s = withContext(Dispatchers.Default) { runCatching { v.serializeState() }.getOrNull() }
                if (s != null && !rewindBuf.push(s)) { tooBig = true; rewindBuf.clear(); toast(Tx.rewindUnavailable) }
                delay(RewindBuffer.INTERVAL_MS)
            }
        }
    }

    private fun setRewinding(on: Boolean) {
        if (rewinding == on) return
        rewinding = on
        view?.audioEnabled = !on && !menu.isOpen
    }

    private fun updateRewindHold() = setRewinding(rewind && gameRunning && !menu.isOpen && (rewindKeyDown || rewindAxisDown))

    private fun applyAspect() {
        val v = view ?: return
        val w = v.width.toFloat(); val h = v.height.toFloat()
        if (w <= 0 || h <= 0) return
        v.pivotX = w / 2; v.pivotY = h / 2
        if (!stretch) { v.scaleX = 1f; v.scaleY = 1f; return }
        // LibretroDroid always letterboxes to the core's aspect ratio; "Esticar" scales the surface so the picture
        // fills the screen (non-uniform scale).
        val content = CoreRegistry.displayAspect(prepared?.platform?.id.orEmpty())
        val screen = w / h
        if (content < screen) { v.scaleX = screen / content; v.scaleY = 1f } else { v.scaleX = 1f; v.scaleY = content / screen }
    }

    // ---- play time & session ------------------------------------------------------------------------

    private fun running() = resumed && gameRunning && !menu.isOpen
    private fun updateClock() {
        val now = SystemClock.elapsedRealtime()
        if (running() && runStart == 0L) runStart = now
        else if (!running() && runStart != 0L) { playedAcc += now - runStart; runStart = 0L }
    }
    private fun playedMs(): Long = playedAcc + if (runStart != 0L) SystemClock.elapsedRealtime() - runStart else 0L

    private fun startSession() = sendSession("start")

    private fun sendSession(state: String) {
        val a = api ?: return
        val p = prepared ?: return
        val played = playedMs()
        AppScope.io.launch {
            val reply = runCatching { a.session(p.item.id, state, p.mediaId, p.core, played) }.getOrNull() ?: return@launch
            if (reply.stop && state != "stop") main.post { toast(Tx.stoppedBySchedule, long = true); exitGame() }
        }
    }

    private fun stopSessionOnce() {
        if (sessionStopped || prepared == null) return
        sessionStopped = true
        sendSession("stop")
    }

    // ---- snapshots & uploads ----------------------------------------------------------------------

    /** Runs [block] on a helper thread and waits at most [ms] (the GL thread may be paused: never hang). */
    private fun <T> timed(ms: Long, block: () -> T?): T? {
        val ref = AtomicReference<T?>(null)
        val t = Thread { ref.set(runCatching(block).getOrNull()) }.apply { isDaemon = true; start() }
        t.join(ms)
        return ref.get()
    }

    private fun captureThumb(v: GLRetroView): ByteArray? {
        if (Build.VERSION.SDK_INT < 24 || v.width <= 0 || v.height <= 0 || !v.holder.surface.isValid) return null
        val tw = 320
        val th = (tw * v.height.toFloat() / v.width).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var ok = false
        val ht = HandlerThread("thumb").apply { start() }
        try {
            PixelCopy.request(v, Rect(0, 0, v.width, v.height), bmp, { r -> ok = r == PixelCopy.SUCCESS; latch.countDown() }, Handler(ht.looper))
            latch.await(1500, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        } finally { ht.quitSafely() }
        if (!ok) return null
        return ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 90, out); out.toByteArray() }
    }

    private fun captureSnapshot(thumb: Boolean): Snapshot {
        val v = view ?: return Snapshot(null, null, null)
        val state = timed(3000) { v.serializeState() }
        val sram = timed(2000) { v.serializeSRAM() }
        val t = if (thumb && state != null) captureThumb(v) else null
        return Snapshot(state, sram, t)
    }

    private fun uploadSramIfChanged(sram: ByteArray) {
        val a = api ?: return
        val p = prepared ?: return
        if (sram.isEmpty() || sram.size > MAX_SRAM || sram.contentEquals(lastSram)) return
        lastSram = sram
        AppScope.io.launch { runCatching { a.putSram(p.item.id, p.core, p.mediaId, sram) }.onFailure { lastSram = null } }
    }

    private fun uploadAuto(s: Snapshot) {
        val a = api ?: return
        val p = prepared ?: return
        s.sram?.let { uploadSramIfChanged(it) }
        val state = s.state ?: return
        if (state.isEmpty() || state.size > MAX_STATE) return
        lastAutosaveAt = SystemClock.elapsedRealtime()
        AppScope.io.launch {
            runCatching {
                val id = a.postState(p.item.id, p.core, p.mediaId, 0, true, null, state)
                s.thumb?.let { a.putThumb(id, it) }
            }
        }
    }

    /** Pause (Home, system dialog, finish): save SRAM + auto state + thumbnail while the GL thread is alive. */
    private fun onLeaving() {
        if (!gameRunning) return
        val recent = SystemClock.elapsedRealtime() - lastAutosaveAt < 10_000
        val snap = captureSnapshot(thumb = !recent)
        if (recent) snap.sram?.let { uploadSramIfChanged(it) } else uploadAuto(snap)
        if (isFinishing) stopSessionOnce()
    }

    // ---- input ------------------------------------------------------------------------------------

    private fun isGamepad(e: KeyEvent) = e.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
        e.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    private fun portOf(e: KeyEvent): Int = ((e.device?.controllerNumber ?: 1) - 1).coerceIn(0, 3)

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val v = view
        if (v == null || !gameRunning) {
            if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP && status.visibility == View.VISIBLE &&
                !exiting && prepared == null) { finishWith(Activity.RESULT_CANCELED); return true }
            return super.dispatchKeyEvent(e)
        }
        val down = e.action == KeyEvent.ACTION_DOWN

        if (menu.isOpen) {
            // Back (or B on a pad) closes the innermost layer; everything else is normal focus navigation.
            if (e.keyCode == KeyEvent.KEYCODE_BACK || e.keyCode == KeyEvent.KEYCODE_BUTTON_B || e.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (!down) menu.back()
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_BUTTON_MODE) { if (!down) resumeGame(); return true }
            return super.dispatchKeyEvent(e)
        }

        // --- game running ---
        if (PadMapper.isMenuKey(e.keyCode)) {
            if (e.keyCode == KeyEvent.KEYCODE_BACK) {
                if (down) backDownHandled = e.repeatCount == 0 else if (backDownHandled) { backDownHandled = false; openMenu() }
            } else if (!down) openMenu()
            return true
        }

        if (PadMapper.isMenuChordKey(e.keyCode) && isGamepad(e)) {
            val isStart = e.keyCode == KeyEvent.KEYCODE_BUTTON_START
            if (down) { if (isStart) startDown = true else selectDown = true } else { if (isStart) startDown = false else selectDown = false }
            if (down && startDown && selectDown) {
                chordConsumed = true
                // release both in the core, then open the menu
                v.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START, portOf(e))
                v.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT, portOf(e))
                openMenu()
                return true
            }
            if (!down && chordConsumed) { if (!startDown && !selectDown) chordConsumed = false; return true }
        }

        if (rewind && e.keyCode == KeyEvent.KEYCODE_BUTTON_L2 && isGamepad(e)) {
            rewindKeyDown = down; updateRewindHold(); return true
        }
        val mapped = PadMapper.gameKey(e.keyCode, isGamepad(e))
        if (mapped != null) {
            if (e.repeatCount == 0) v.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, mapped, portOf(e))
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        val v = view
        if (v != null && gameRunning && !menu.isOpen && e.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            e.action == MotionEvent.ACTION_MOVE) {
            if (rewind) {
                val lt = maxOf(e.getAxisValue(MotionEvent.AXIS_LTRIGGER), e.getAxisValue(MotionEvent.AXIS_BRAKE)) > 0.5f
                if (lt != rewindAxisDown) { rewindAxisDown = lt; updateRewindHold() }
            }
            val ax = PadMapper.dpadAxis(e.getAxisValue(MotionEvent.AXIS_X), e.getAxisValue(MotionEvent.AXIS_HAT_X))
            val ay = PadMapper.dpadAxis(e.getAxisValue(MotionEvent.AXIS_Y), e.getAxisValue(MotionEvent.AXIS_HAT_Y))
            if (ax != lastSentAxisX || ay != lastSentAxisY) {
                lastSentAxisX = ax; lastSentAxisY = ay
                v.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, ax, ay, ((e.device?.controllerNumber ?: 1) - 1).coerceIn(0, 3))
            }
            return true
        }
        return super.dispatchGenericMotionEvent(e)
    }

    // ---- menu host --------------------------------------------------------------------------------

    private fun openMenu() {
        val v = view ?: return
        if (menu.isOpen) return
        rewindKeyDown = false; rewindAxisDown = false; setRewinding(false)
        v.frameSpeed = 0
        v.audioEnabled = false
        menu.open()
        updateClock()
    }

    override fun resumeGame() {
        menu.close()
        view?.let { it.frameSpeed = if (fast) FAST_SPEED else 1; it.audioEnabled = true }
        updateClock()
    }

    override fun toggleFast() { fast = !fast }
    override fun toggleStretch() { stretch = !stretch; prefs.edit().putBoolean("stretch", stretch).apply(); applyAspect() }
    private fun pid() = prepared?.platform?.id.orEmpty()

    override fun cycleFilter() {
        filter = filter.next()
        prefs.edit().putString("filter_${pid()}", filter.id).apply()
        view?.shader = filter.config()
    }

    override fun toggleRewind() {
        rewind = !rewind
        rewindBuf.clear()
        prefs.edit().putBoolean("rewind_${pid()}", rewind).apply()
    }

    override fun changeDisc(index: Int) {
        val v = view ?: return
        if (index == currentDisc || index !in discs.indices) return
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.Default) { runCatching { v.changeDisk(index); true }.getOrDefault(false) }
            if (ok) currentDisc = index
            toast(if (ok) Tx.discChanged else Tx.discFailed)
            if (ok) resumeGame()
        }
    }

    override fun restartGame() {
        rewindBuf.clear()
        view?.reset()
        resumeGame()
    }

    override fun exitGame() {
        if (exiting) return
        exiting = true
        menu.close()
        finishWith(Activity.RESULT_OK)
    }

    private fun finishWith(code: Int) {
        updateClock()
        setResult(code, Intent().putExtra(EXTRA_PLAYED_MS, playedMs()))
        finish()
    }

    @Deprecated("Back is routed through dispatchKeyEvent")
    override fun onBackPressed() { /* handled in dispatchKeyEvent */ }

    override fun saveToSlot(slot: Int) {
        val p = prepared ?: return
        val a = api ?: return
        val v = view ?: return
        lifecycleScope.launch {
            val snap = withContext(Dispatchers.Default) { captureSnapshot(thumb = true) }
            val state = snap.state
            if (state == null || state.isEmpty() || state.size > MAX_STATE) { toast(Tx.saveFailed); return@launch }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val id = a.postState(p.item.id, p.core, p.mediaId, slot, false, "Slot $slot", state)
                    snap.thumb?.let { a.putThumb(id, it) }
                    true
                }.getOrDefault(false)
            }
            snap.sram?.let { uploadSramIfChanged(it) }
            menu.clearThumbs()
            toast(if (ok) Tx.savedToast else Tx.saveFailed)
            if (ok) resumeGame()
            v.requestLayout()
        }
    }

    override fun loadFromState(state: SaveState) {
        val a = api ?: return
        val v = view ?: return
        if (state.core != currentCore) { toast(Tx.wrongCore); return }
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { v.unserializeState(a.state(state.id)) }.getOrDefault(false)
            }
            toast(if (ok) Tx.loadedToast else Tx.loadFailed)
            if (ok) { rewindBuf.clear(); resumeGame() }
        }
    }

    override suspend fun fetchSaves(): Saves? = withContext(Dispatchers.IO) { runCatching { api?.saves(req!!.itemId) }.getOrNull() }
    override suspend fun fetchThumb(url: String): ByteArray? = withContext(Dispatchers.IO) { runCatching { api?.thumb(url) }.getOrNull() }

    private fun toast(text: String, long: Boolean = false) {
        Toast.makeText(this, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val HEARTBEAT_MS = 30_000L
        private const val SRAM_MS = 60_000L
        private const val AUTOSAVE_MS = 5 * 60_000L
        private const val MAX_SRAM = 2 * 1024 * 1024
        private const val MAX_STATE = 32 * 1024 * 1024
        private const val FAST_SPEED = 2
    }
}

private suspend fun coroutineScope2(block: suspend CoroutineScope.() -> Unit) = kotlinx.coroutines.coroutineScope(block)
