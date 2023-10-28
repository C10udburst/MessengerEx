package io.github.cloudburst.messengerex

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

const val TAG = "MessengerExPatches"

fun removeAds(cl: ClassLoader, bridge: DexKitBridge) {
    try {
        val classData = bridge.findClass {
            searchPackages("com.facebook.messaging.business.inboxads.plugins.inboxads.itemsupplier")
            matcher {
                className("com.facebook.messaging.business.inboxads.plugins.inboxads.itemsupplier.InboxAdsItemSupplierImplementation")
            }
        }.firstOrNull() ?: return

        val method = bridge.findMethod {
            searchInClass(listOf(classData))
            matcher {
                returnType("void")
                usingStrings("ads_load_begin", "inbox_ads_fetch_start")
                modifiers(Modifier.PUBLIC or Modifier.STATIC)
            }
        }.firstOrNull() ?: return

        Log.i(TAG, "Found method: $method")
        XposedBridge.hookMethod(method.getMethodInstance(cl), XC_MethodReplacement.DO_NOTHING)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to remove ads", e)
    }
}

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
        }.firstOrNull() ?: return

        Log.i(TAG, "Found method: $method")
        XposedBridge.hookMethod(method.getMethodInstance(cl), object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                val ctx = param?.args?.get(0) as? Context ?: return
                val url = param.args?.get(1) as? Uri ?: return

                val intent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                intent.launchUrl(ctx, url)
                param.result = null
            }
        })
    } catch (e: Exception) {
        Log.e(TAG, "Failed to replace browser", e)
    }
}
