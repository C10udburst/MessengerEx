package io.github.cloudburst.messengerex.patches

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

val loggerServices = listOf(
    "AlarmBasedUploadService",
    "GooglePlayUploadService",
    "LollipopUploadService"
)

fun removeService(cl: ClassLoader, className: String) {
    val clazz = cl.loadClass("com.facebook.analytics2.logger.${className}")
    val method = clazz.declaredMethods.firstOrNull {
        it.name == "onStartCommand" && it.returnType == Int::class.javaPrimitiveType
    } ?: return
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam?) {
            param?.result = 2
        }
    })
    Log.d(TAG, "Removed service $className")
}


fun removeServices(cl: ClassLoader) {
    loggerServices.forEach {
        try {
            removeService(cl, it)
        } catch (e: Exception) {
            XposedBridge.log("Failed to remove service $it. Reason: ${e.message}")
        }
    }
}