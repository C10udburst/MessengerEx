package io.github.cloudburst.messengerex;

import static io.github.cloudburst.messengerex.UtilsKt.getApkPath;
import static io.github.cloudburst.messengerex.patches.AnalyticsKt.*;
import static io.github.cloudburst.messengerex.patches.InboxKt.*;
import static io.github.cloudburst.messengerex.patches.MessagesKt.*;

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
        try (DexKitBridge bridge = DexKitBridge.create(getApkPath(lpparam.appInfo))) {
            if (bridge == null) {
                Log.e(TAG, "Failed to create DexKitBridge");
                return;
            }

            removeAds(cl, bridge);
            replaceBrowser(cl, bridge);
            removeServices(cl);
        } catch (Exception e) {
            Log.e(TAG, "Failed to find method", e);
        }
    }

}
