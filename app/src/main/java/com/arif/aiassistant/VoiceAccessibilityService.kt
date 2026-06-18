package com.arif.aiassistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VoiceAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Screen a ja change hocche tar event ekhane ashe.
        // Ei function extend kore "screen a X dekha gele Y koro" type automation
        // future a banano jay.
    }

    override fun onInterrupt() {}

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Current screen a kono matching text khuje pele oi element a click kore.
     * "Sora, settings option a click koro" type voice command er jonno use hoy.
     */
    fun findAndClickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNode(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findNode(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString()
        if (nodeText != null && nodeText.contains(text, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNode(child, text)
            if (found != null) return found
        }
        return null
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
