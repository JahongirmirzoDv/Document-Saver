// commonMain/kotlin/uz/mobiledv/test1/util/Platform.kt
package uz.mobiledv.test1.util

enum class PlatformType {
    ANDROID, DESKTOP, UNKNOWN
}

expect fun getCurrentPlatform(): PlatformType