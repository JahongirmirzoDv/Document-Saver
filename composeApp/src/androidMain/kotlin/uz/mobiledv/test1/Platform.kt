package uz.mobiledv.test1

actual object Platform {
    actual fun isDesktop(): Boolean = false
    actual fun isAndroid(): Boolean = true
} 