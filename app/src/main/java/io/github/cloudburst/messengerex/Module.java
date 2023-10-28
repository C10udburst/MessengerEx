package io.github.cloudburst.messengerex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.*;

import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.*;
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

            removeAds(cl, bridge);
        } catch (Exception e) {
            Log.e(TAG, "Failed to find method", e);
        }
        useNormalBrowser(cl);
    }

    private void removeAds(ClassLoader cl, DexKitBridge bridge) throws NoSuchMethodException {
        var classData = bridge.findClass(
                FindClass.create()
                        .searchPackages("com.facebook.messaging.business.inboxads.plugins.inboxads.itemsupplier")
                        .matcher(ClassMatcher.create().className(
                                "com.facebook.messaging.business.inboxads.plugins.inboxads.itemsupplier.InboxAdsItemSupplierImplementation"
                        ))
        ).first();

        var method = bridge.findMethod(
                FindMethod.create()
                        .searchInClass(List.of(classData))
                        .matcher(MethodMatcher.create()
                                .returnType("void")
                                .usingStrings("ads_load_begin", "inbox_ads_fetch_start")
                                .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                        )
        ).first();
        Log.i(TAG, "Found method: " + method);

        XposedBridge.hookMethod(method.getMethodInstance(cl), XC_MethodReplacement.DO_NOTHING);
    }

    private void useNormalBrowser(ClassLoader cl) throws ClassNotFoundException, NoSuchMethodException {
        XposedBridge.hookMethod(
                cl.loadClass("com.facebook.browser.lite.BrowserLiteActivity").getDeclaredMethod("onCreate", Bundle.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        var ctx = (Activity) param.thisObject;
                        var intent = ctx.getIntent();
                        Uri data = intent.getData();

                        // create new intent, that opens the URL in the default browser using chrome custom tabs
                        (new CustomTabsIntent.Builder())
                            .build()
                            .launchUrl(ctx, data);

                        ctx.finishAfterTransition();
                    }
                }
        );
    }
}
