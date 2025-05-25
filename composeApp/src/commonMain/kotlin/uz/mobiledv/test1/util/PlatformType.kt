package uz.mobiledv.test1.util

enum class PlatformType {
    ANDROID, DESKTOP, UNKNOWN
}

expect fun getCurrentPlatform(): PlatformType
