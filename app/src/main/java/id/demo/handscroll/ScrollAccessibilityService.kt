package id.demo.handscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun swipeUp() {
        val dm = resources.displayMetrics
        val path = Path().apply {
            moveTo(dm.widthPixels * 0.5f, dm.heightPixels * 0.78f)
            lineTo(dm.widthPixels * 0.5f, dm.heightPixels * 0.28f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 380))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    companion object {
        @Volatile var instance: ScrollAccessibilityService? = null
            private set
        @Volatile var enabledByUser = false
        fun requestSwipe() { if (enabledByUser) instance?.swipeUp() }
    }
}
