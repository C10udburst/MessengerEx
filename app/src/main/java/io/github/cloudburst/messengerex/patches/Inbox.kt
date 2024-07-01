package io.github.cloudburst.messengerex.patches

import android.util.Log
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

fun removeAds(cl: ClassLoader, bridge: DexKitBridge) {
    try {
        val classData = bridge.findClass {
            searchPackages("com.facebook.messaging.business.inboxads.plugins.inboxads.itemsupplier")
            matcher {
                className("com.facebook.messaging.business.inboxads.plugins.inboxads.itemsupplier.InboxAdsItemSupplierImplementation")
            }
        }.first()

        val method = bridge.findMethod {
            searchInClass(listOf(classData))
            matcher {
                returnType("void")
                usingStrings("ads_load_begin", "inbox_ads_fetch_start")
                modifiers(Modifier.PUBLIC or Modifier.STATIC)
            }
        }.first()

        Log.i(TAG, "Found method: $method")
        XposedBridge.hookMethod(method.getMethodInstance(cl), XC_MethodReplacement.DO_NOTHING)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to remove ads", e)
    }
}
