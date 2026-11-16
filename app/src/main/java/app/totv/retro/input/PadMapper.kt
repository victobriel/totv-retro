package app.totv.retro.input

import android.view.KeyEvent

/**
 * Android key events -> libretro joypad. LibretroDroid's `sendKeyEvent` expects "RetroPad" semantics expressed
 * as Android key codes, i.e. KEYCODE_BUTTON_A = RetroPad A (east) and KEYCODE_BUTTON_B = RetroPad B (south).
 * Android gamepads (Xbox layout) are the other way round, so A/B and X/Y are swapped (east = A, like a SNES pad).
 */
object PadMapper {
    private val dpad = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    /**
     * @return the key code to forward to the core, or null when the key is not a game key
     * (Back, Home, Guide... are handled by the app).
     */
    fun gameKey(keyCode: Int, fromGamepad: Boolean): Int? = when (keyCode) {
        in dpad -> keyCode
        KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
        KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
        KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
        KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT -> keyCode
        // Fire TV / Android TV remote fallback: OK = A, Play/Pause = Start, Menu = Select
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
            if (fromGamepad) null else KeyEvent.KEYCODE_BUTTON_A
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE ->
            KeyEvent.KEYCODE_BUTTON_START
        KeyEvent.KEYCODE_MENU -> KeyEvent.KEYCODE_BUTTON_SELECT
        else -> null
    }

    /** True for the keys that, held together (Select + Start), open the menu. */
    fun isMenuChordKey(keyCode: Int) = keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT

    /** Keys that open the menu on their own (Back on any device, Guide button on pads). */
    fun isMenuKey(keyCode: Int) = keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_MODE

    /**
     * Left stick / hat -> digital D-pad direction in {-1, 0, 1}. The hat wins when pressed; the stick needs
     * to pass [dead] (0..1).
     */
    fun dpadAxis(stick: Float, hat: Float, dead: Float = 0.5f): Float = when {
        hat > 0.5f -> 1f
        hat < -0.5f -> -1f
        stick > dead -> 1f
        stick < -dead -> -1f
        else -> 0f
    }
}
