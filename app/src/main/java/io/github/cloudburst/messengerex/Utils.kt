package io.github.cloudburst.messengerex

import android.content.pm.ApplicationInfo
import android.net.Uri

fun getApkPath(apkinfo: ApplicationInfo): String = apkinfo.splitSourceDirs?.firstOrNull {
    it.contains("split_d_longtail_raw_v9")
} ?: apkinfo.sourceDir

fun removeTrackingUrl(uri: Uri): Uri {
    var url = uri
    if (url.host == "l.facebook.com" && url.path == "/l.php") {
        url = Uri.parse(url.getQueryParameter("u"))
    }
    val builder = url.buildUpon()
    builder.clearQuery()
    val cleanQuery = url.queryParameterNames
        .filterNot { it.equals("fbclid", ignoreCase = true) }
        .filterNot { it.startsWith("utm_", ignoreCase = true) }
        .filterNot {it.equals("si", ignoreCase = true)}
        .map { it to url.getQueryParameter(it) }
    cleanQuery.forEach { (key, value) ->
        builder.appendQueryParameter(key, value)
    }
    return builder.build()
}