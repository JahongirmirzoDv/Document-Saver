package uz.mobiledv.test1.util

enum class PlatformType {
    ANDROID, DESKTOP, IOS, UNKNOWN
}

expect fun getCurrentPlatform(): PlatformType
