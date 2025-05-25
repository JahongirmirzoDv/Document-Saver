package uz.mobiledv.test1.model

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import uz.mobiledv.test1.MyActivity // Assuming your Application class with context
import java.io.File

@RequiresApi(Build.VERSION_CODES.P)
actual fun getCurrentAppVersionCode(): Int {
    if (!MyActivity.AppContextHolder.isInitialized()) return 0 // Or handle error
    val context = MyActivity.AppContextHolder.appContext
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.longVersionCode.toInt()
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        0 // Should not happen
    }
}

actual fun getCurrentAppVersionName(): String {
    if (!MyActivity.AppContextHolder.isInitialized()) return "0.0.0" // Or handle error
    val context = MyActivity.AppContextHolder.appContext
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "Unknown" // Handle case where versionName might be null
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        "Unknown" // Should not happen
    }
}

actual fun triggerApkInstall(apkFilePath: String) {
    if (!MyActivity.AppContextHolder.isInitialized()) {
        println("Error: App context not initialized for APK install.")
        return
    }
    val context = MyActivity.AppContextHolder.appContext
    val file = File(apkFilePath)
    if (!file.exists()) {
        println("Error: APK file not found at $apkFilePath")
        return
    }

    val intent = Intent(Intent.ACTION_VIEW)
    val uri: Uri

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        uri = Uri.fromFile(file)
    }

    intent.setDataAndType(uri, "application/vnd.android.package-archive")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // Check if the system can handle this intent
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        println("Error: No activity found to handle APK installation.")
        // You might want to show a message to the user here
    }
}
