package io.github.cloudburst.messengerex;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Module implements IXposedHookLoadPackage {
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d("MsgEx", "Loaded package: " + lpparam.packageName);
        if (!lpparam.packageName.equals("com.facebook.orca")) return;

        var cl = lpparam.classLoader;
        var main = cl.loadClass("com.facebook.orca.auth.StartScreenActivity");
        XposedHelpers.findAndHookMethod(main, "onCreate", new XC_MethodHook() {
            @SuppressWarnings("JavaReflectionInvocation")
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var ctx = (Context) param.thisObject;
                System.loadLibrary("dexkit");
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    Toast.makeText(ctx, "Dexkit loaded", Toast.LENGTH_LONG).show();
                    Log.d("MsgEx", "Dexkit loaded: " + bridge);
                }
            }
        });
    }
}
