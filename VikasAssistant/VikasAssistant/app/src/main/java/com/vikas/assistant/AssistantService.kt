package com.vikas.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Sirf Settings screen par "Force stop" dabata hai (app close karne ke liye). Koi data store/send nahi. */
class AssistantService : AccessibilityService() {
    companion object {
        var instance: AssistantService? = null
        @Volatile var pending = false

        fun forceStop(ctx: android.content.Context, pkg: String): Boolean {
            if (instance == null) return false
            pending = true
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
    }
    private val stopLabels = listOf("Force stop", "Force Stop", "जबरदस्ती रोकें", "ज़बरदस्ती रोकें", "बलपूर्वक रोकें")

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (!pending || e?.packageName != "com.android.settings") return
        val root = rootInActiveWindow ?: return
        // Confirm dialog (OK)
        root.findAccessibilityNodeInfosByViewId("android:id/button1").firstOrNull()?.let {
            if (it.isEnabled && click(it)) { pending = false; performGlobalAction(GLOBAL_ACTION_HOME); return }
        }
        for (l in stopLabels) {
            val n = root.findAccessibilityNodeInfosByText(l).firstOrNull() ?: continue
            if (!n.isEnabled) { pending = false; performGlobalAction(GLOBAL_ACTION_HOME); return } // already stopped
            if (click(n)) return
        }
    }
    private fun click(n: AccessibilityNodeInfo): Boolean {
        var c: AccessibilityNodeInfo? = n
        while (c != null) { if (c.isClickable) return c.performAction(AccessibilityNodeInfo.ACTION_CLICK); c = c.parent }
        return false
    }
}
