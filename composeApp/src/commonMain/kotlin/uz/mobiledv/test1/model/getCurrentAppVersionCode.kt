package uz.mobiledv.test1.model

/**
 * Gets the current version code of the application.
 */
expect fun getCurrentAppVersionCode(): Int

/**
 * Gets the current version name of the application.
 */
expect fun getCurrentAppVersionName(): String

/**
 * Triggers the APK installation process.
 * This is a complex operation and might require further user interaction
 * and permissions (e.g., "Install from Unknown Sources").
 *
 * @param apkFilePath The local file path to the downloaded APK.
 */
expect fun triggerApkInstall(apkFilePath: String)