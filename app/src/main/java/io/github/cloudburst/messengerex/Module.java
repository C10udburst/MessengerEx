package io.github.cloudburst.messengerex;

import android.util.Log;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Module implements IXposedHookLoadPackage {

    private static final String TAG = "MessengerEx";

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.facebook.orca")) return;


        var cl = lpparam.classLoader;
        System.loadLibrary("dexkit");
        try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
            if (bridge == null) {
                Log.e(TAG, "Failed to create DexKitBridge");
                return;
            }

            PatchesKt.removeAds(cl, bridge);
            PatchesKt.replaceBrowser(cl, bridge);
        } catch (Exception e) {
            Log.e(TAG, "Failed to find method", e);
        }
    }

}
