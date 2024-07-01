package io.github.cloudburst.messengerex.patches

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.cloudburst.messengerex.removeTrackingUrl
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

fun replaceBrowser(cl: ClassLoader, bridge: DexKitBridge) {
    try {
        val method = bridge.findMethod {
            matcher {
                returnType("void")
                addParamType("android.content.Context")
                addParamType("android.net.Uri")
                addParamType("com.facebook.xapp.messaging.browser.model.MessengerInAppBrowserLaunchParam")
                modifiers(Modifier.PUBLIC)
            }
        }.first()

        Log.i(TAG, "Found method: $method")
        XposedBridge.hookMethod(method.getMethodInstance(cl), object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val ctx = param?.args?.get(0) as? Context ?: return
                val url = param.args?.get(1) as? Uri ?: return
                val cleanUrl = removeTrackingUrl(url)

                val intent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                intent.launchUrl(ctx, cleanUrl)
                param.result = null
            }
        })
    } catch (e: Exception) {
        Log.e(TAG, "Failed to replace browser", e)
    }
}