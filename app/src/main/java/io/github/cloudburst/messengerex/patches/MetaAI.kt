package io.github.cloudburst.messengerex.patches

import android.content.pm.ApplicationInfo
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import java.io.File

private const val META_AI_KILL_SWITCH = "SearchAiagentImplementationsKillSwitch"
private const val META_AI_PREFIX_LENGTH = 7

fun removeMetaAI(cl: ClassLoader, bridge: DexKitBridge, appInfo: ApplicationInfo) {
    try {
        val killSwitchMethods = bridge.findMethod {
            matcher {
                usingStrings(META_AI_KILL_SWITCH)
            }
        }

        if (killSwitchMethods.isEmpty()) {
            Log.w(TAG, "Meta AI kill switch method not found")
            return
        }

        killSwitchMethods.forEach { methodData ->
            if (!methodData.isMethod) {
                return@forEach
            }
            val method = methodData.getMethodInstance(cl)
            val returnType = method.returnType
            if (returnType != Boolean::class.javaPrimitiveType && returnType != java.lang.Boolean::class.java) {
                return@forEach
            }
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
        }

        val prefixes = extractMetaAiPrefixes(killSwitchMethods, appInfo)
        if (prefixes.isEmpty()) {
            Log.w(TAG, "Meta AI flag prefix not found")
            return
        }

        val mobileConfigClasses = bridge.findClass {
            matcher {
                addInterface("com.facebook.mobileconfig.factory.MobileConfigUnsafeContext")
            }
        }

        if (mobileConfigClasses.isEmpty()) {
            Log.w(TAG, "MobileConfigUnsafeContext implementation not found")
            return
        }

        val boolGetters = bridge.findMethod {
            searchInClass(mobileConfigClasses)
            matcher {
                returnType("boolean")
                paramCount(1)
                addParamType("long")
            }
        }

        if (boolGetters.isEmpty()) {
            Log.w(TAG, "Mobile config boolean getter not found")
            return
        }

        boolGetters.forEach { methodData ->
            val method = methodData.getMethodInstance(cl)
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val flagId = param?.args?.get(0) as? Long ?: return
                    val digits = flagId.toString()
                    for (prefix in prefixes) {
                        if (digits.startsWith(prefix)) {
                            param.result = false
                            return
                        }
                    }
                }
            })
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to remove Meta AI", e)
    }
}

private fun extractMetaAiPrefixes(
    killSwitchMethods: List<MethodData>,
    appInfo: ApplicationInfo
): Set<String> {
    if (killSwitchMethods.isEmpty()) {
        return emptySet()
    }

    val classDescriptors = killSwitchMethods
        .map { it.className }
        .map { "L" + it.replace('.', '/') + ";" }
        .toSet()

    val candidateLiterals = mutableListOf<Long>()
    val apkPaths = buildList {
        add(appInfo.sourceDir)
        appInfo.splitSourceDirs?.let { addAll(it) }
    }.distinct()

    for (apkPath in apkPaths) {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            continue
        }

        val container = DexFileFactory.loadDexContainer(apkFile, Opcodes.getDefault())
        for (entryName in container.dexEntryNames) {
            val dexEntry = container.getEntry(entryName) ?: continue
            val dexFile = dexEntry.dexFile
            for (classDef in dexFile.classes) {
                if (!classDescriptors.contains(classDef.type)) {
                    continue
                }
                for (method in classDef.methods) {
                    val impl = method.implementation ?: continue
                    for (instruction in impl.instructions) {
                        val literal = (instruction as? WideLiteralInstruction)?.wideLiteral
                        if (literal != null && literal > 0) {
                            candidateLiterals.add(literal)
                        }
                    }
                }
            }
        }
    }

    if (candidateLiterals.isEmpty()) {
        return emptySet()
    }

    val prefixes = candidateLiterals
        .map { it.toString() }
        .filter { it.length >= META_AI_PREFIX_LENGTH }
        .map { it.substring(0, META_AI_PREFIX_LENGTH) }
        .toSet()

    if (prefixes.isEmpty()) {
        Log.w(TAG, "Meta AI literals found but no valid prefixes: $candidateLiterals")
    }

    return prefixes
}
