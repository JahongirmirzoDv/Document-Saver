package uz.mobiledv.test1.model

import platform.Foundation.NSBundle

actual fun getCurrentAppVersionCode(): Int {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")?.toString()?.toIntOrNull() ?: 0
}

actual fun getCurrentAppVersionName(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")?.toString() ?: "Unknown"
}

actual fun triggerApkInstall(apkFilePath: String) {
    // This is an Android-specific feature and has no equivalent on iOS.
    // You can leave this empty or log a message.
    println("triggerApkInstall is not supported on iOS.")
}