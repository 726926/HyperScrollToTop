package com.hyper.scrolltop

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val PACKAGE_NAME = "com.hyper.scrolltop"
        private const val ACTION_SCROLL_TOP = "com.hyper.scrolltop.ACTION_SCROLL_TO_TOP"
        private const val PREF_KEY_ENABLE = "enable_scroll_top"
    }

    private fun isModuleEnabled(): Boolean {
        return try {
            val pref = XSharedPreferences(PACKAGE_NAME, "settings")
            pref.makeWorldReadable()
            pref.reload()
            pref.getBoolean(PREF_KEY_ENABLE, true)
        } catch (e: Throwable) {
            true
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. Hook HyperOS 状态栏点击发送广播
        if (lpparam.packageName == "com.android.systemui") {
            hookSystemUIStatusBar(lpparam)
        }

        // 2. Hook 前台 Activity 接收广播并执行平滑滚动回顶
        hookActivityScroll(lpparam)
    }

    private fun hookSystemUIStatusBar(lpparam: XC_LoadPackage.LoadPackageParam) {
        val statusBarClasses = arrayOf(
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            "com.android.systemui.statusbar.window.StatusBarWindowView"
        )

        for (className in statusBarClasses) {
            try {
                XposedHelpers.findAndHookMethod(
                    className,
                    lpparam.classLoader,
                    "onTouchEvent",
                    MotionEvent::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!isModuleEnabled()) return
                            val event = param.args[0] as? MotionEvent ?: return
                            if (event.action == MotionEvent.ACTION_UP) {
                                val view = param.thisObject as? View ?: return
                                val intent = Intent(ACTION_SCROLL_TOP)
                                view.context.sendBroadcast(intent)
                            }
                        }
                    }
                )
            } catch (_: Throwable) {}
        }
    }

    private fun hookActivityScroll(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        val decorView = activity.window?.decorView ?: return
                        
                        val tagKey = 0x7f010088
                        if (decorView.getTag(tagKey) == true) return

                        val receiver = object : BroadcastReceiver() {
                            override fun onReceive(context: Context?, intent: Intent?) {
                                if (intent?.action == ACTION_SCROLL_TOP && isModuleEnabled()) {
                                    scrollToTopRecursive(decorView)
                                }
                            }
                        }

                        val filter = IntentFilter(ACTION_SCROLL_TOP)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                        } else {
                            activity.registerReceiver(receiver, filter)
                        }

                        decorView.setTag(tagKey, true)
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    private fun scrollToTopRecursive(view: View): Boolean {
        if (!view.isShown) return false
        when (view) {
            is RecyclerView -> { view.smoothScrollToPosition(0); return true }
            is ScrollView -> { view.smoothScrollTo(0, 0); return true }
            is NestedScrollView -> { view.smoothScrollTo(0, 0); return true }
            is AbsListView -> { view.smoothScrollToPosition(0); return true }
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    if (scrollToTopRecursive(view.getChildAt(i))) return true
                }
            }
        }
        return false
    }
}
